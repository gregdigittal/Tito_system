package cash.ice.api.service.impl;

import cash.ice.api.config.property.OtpProperties;
import cash.ice.api.dto.OtpData;
import cash.ice.api.dto.OtpType;
import cash.ice.api.service.NotificationService;
import cash.ice.api.service.OtpService;
import cash.ice.api.service.SecurityPvvService;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.error.ApiValidationException;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.EntityMsisdn;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.EntityMsisdnRepository;
import cash.ice.sqldb.repository.EntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static cash.ice.common.error.ErrorCodes.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@RequiredArgsConstructor
@Profile(IceCashProfile.PROD)
@Slf4j
public class OtpServiceImpl implements OtpService {
    protected final NotificationService notificationService;
    protected final MongoTemplate mongoTemplate;
    protected final SecurityPvvService securityPvvService;
    protected final EntityMsisdnRepository entityMsisdnRepository;
    protected final EntityRepository entityRepository;
    protected final AccountRepository accountRepository;
    protected final OtpProperties otpProperties;

    @Override
    public OtpData sendOtpToAccount(OtpType otpType, String accountNumber, int digitsAmount, boolean resend) {
        Account account = accountRepository.findByAccountNumber(accountNumber).stream().findFirst().orElseThrow(() ->
                new ICEcashException(String.format("Account %s does not exist", accountNumber), EC1022));
        EntityClass entity = entityRepository.findById(account.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + account.getEntityId() + " does not exist", EC1048));
        EntityMsisdn mobile = entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(entity.getId())
                .orElseThrow(() -> new ICEcashException("No primary msisdn on entity: " + entity.getId(), EC1046));
        return sendOtp(otpType, mobile.getMsisdn(), entity.getId(), digitsAmount, resend);
    }

    @Override
    public OtpData sendOtp(OtpType otpType, int entityId, int digitsAmount, boolean resend) {
        EntityMsisdn mobile = entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(entityId)
                .orElseThrow(() -> new ICEcashException("No primary msisdn on entity: " + entityId, EC1046));
        return sendOtp(otpType, mobile.getMsisdn(), entityId, digitsAmount, resend);
    }

    @Override
    public OtpData sendOtp(OtpType otpType, String msisdn, int digitsAmount, boolean resend) {
        return sendOtp(otpType, msisdn, null, digitsAmount, resend);
    }

    private OtpData sendOtp(OtpType otpType, String msisdn, Integer entityId, int digitsAmount, boolean resend) {
        String otp = Tool.generateDigits(digitsAmount, false);
        OtpData otpData = getOtpData(where("otpType").is(otpType).andOperator(where("msisdn").is(msisdn)));
        if (resend) {
            if (otpData == null) {
                throw new ICEcashException("No OTP code is assigned to msisdn: " + msisdn, ErrorCodes.EC1033);
            } else if (isEarlyResend(otpData.getCreatedDate(), otpType)) {
                throw new ICEcashException("Too short time after OTP send", ErrorCodes.EC1081);
            } else {
                otp = securityPvvService.restorePin(otpData.getOtpKey(), otpData.getOtpPvv(), digitsAmount);
            }
        } else {
            if (otpData == null) {
                otpData = new OtpData();
            }
            String otpKey = Tool.generateDigits(16, true);
            otpData = mongoTemplate.save(otpData
                            .setOtpType(otpType)
                            .setMsisdn(msisdn)
                            .setEntityId(entityId)
                            .setOtpKey(otpKey)
                            .setOtpPvv(securityPvvService.acquirePvv(otpKey, otp))
                            .setCreatedDate(Instant.now()),
                    otpProperties.getDataCollection());
        }
        notificationService.sendSmsMessage(otp, msisdn);
        return otpData;
    }

    private boolean isEarlyResend(Instant sendTime, OtpType otpType) {
        if (otpType == OtpType.MOZ_MSISDN_UPDATE) {
            Instant threshold = Instant.now().minus(otpProperties.getResendMsisdnChangeAfter());
            return sendTime.isAfter(threshold);
        } else {
            return false;
        }
    }

    @Override
    public void validateOtp(OtpType otpType, Integer entityId, String otp) {
        validateOtp(getOtpData(where("otpType").is(otpType).andOperator(where("entityId").is(entityId))), otp);
    }

    @Override
    public void validateOtp(OtpType otpType, String msisdn, String otp) {
        validateOtp(getOtpData(where("otpType").is(otpType).andOperator(where("msisdn").is(msisdn))), otp);
    }

    @Override
    public String restorePin(OtpType otpType, String msisdn, Integer entityId, String accountNumber) {
        Criteria criteria;
        if (msisdn != null) {
            criteria = where("msisdn").is(msisdn);
        } else if (entityId != null) {
            criteria = where("entityId").is(entityId);
        } else if (accountNumber != null) {
            Account account = accountRepository.findByAccountNumber(accountNumber).stream().findFirst().orElseThrow(() ->
                    new ICEcashException(String.format("Account %s does not exist", accountNumber), EC1022));
            EntityClass entity = entityRepository.findById(account.getEntityId())
                    .orElseThrow(() -> new ICEcashException("Entity with id=" + account.getEntityId() + " does not exist", EC1048));
            criteria = where("entityId").is(entity.getId());
        } else {
            throw new ICEcashException("At least 1 criteria (msisdn, entityId, accountNumber) must be provided", EC1001);
        }
        OtpData otpData = getOtpData(where("otpType").is(otpType).andOperator(criteria));
        if (otpData != null) {
            return securityPvvService.restorePin(otpData.getOtpKey(), otpData.getOtpPvv(), 4);
        } else {
            throw new RuntimeException("No OTP data available");
        }
    }

    private void validateOtp(OtpData otpData, String otp) {
        if (otpData == null || otp == null) {
            throw new ApiValidationException("OTP required", EC1052);
        } else if (!otp.matches("^[0-9]+$") || !Objects.equals(securityPvvService.acquirePvv(otpData.getOtpKey(), otp), otpData.getOtpPvv())) {
//            mongoTemplate.remove(otpData, OTP_DATA_COLLECTION);
            throw new ApiValidationException("Invalid OTP: " + otp, EC1052);
        } else if (otpData.getCreatedDate().isBefore(Instant.now().minus(otpProperties.getRequestExpirationDuration()))) {
            throw new ApiValidationException("Expired OTP: " + otp, EC1052);
        }
        mongoTemplate.remove(otpData, otpProperties.getDataCollection());
    }

    protected OtpData getOtpData(Criteria searchCriteria) {
        return mongoTemplate.findOne(query(searchCriteria), OtpData.class, otpProperties.getDataCollection());
    }

    @Override
    public void cleanupExpiredOtpDataTask() {
        Instant threshold = Instant.now().minus(otpProperties.getRequestExpirationDuration());
        List<OtpData> expiredData = mongoTemplate.findAll(OtpData.class).stream()
                .filter(d -> d.getCreatedDate() == null || d.getCreatedDate().isBefore(threshold)).toList();
        if (!expiredData.isEmpty()) {
            log.info("Cleanup {} expired otp requests older than: {}", expiredData.size(), threshold);
            List<String> ids = expiredData.stream().map(OtpData::getId).toList();
            mongoTemplate.remove(query(where("_id").in(ids)), otpProperties.getDataCollection());
        }
    }
}
