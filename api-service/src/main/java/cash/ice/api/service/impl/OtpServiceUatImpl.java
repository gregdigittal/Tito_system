package cash.ice.api.service.impl;

import cash.ice.api.config.property.OtpProperties;
import cash.ice.api.dto.OtpData;
import cash.ice.api.dto.OtpType;
import cash.ice.api.service.NotificationService;
import cash.ice.api.service.SecurityPvvService;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.EntityMsisdnRepository;
import cash.ice.sqldb.repository.EntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class OtpServiceUatImpl extends OtpServiceImpl {
    private final List<String> magicPasswords = List.of("0000");

    public OtpServiceUatImpl(NotificationService notificationService, MongoTemplate mongoTemplate, SecurityPvvService securityPvvService, EntityMsisdnRepository entityMsisdnRepository, EntityRepository entityRepository, AccountRepository accountRepository, OtpProperties otpProperties) {
        super(notificationService, mongoTemplate, securityPvvService, entityMsisdnRepository, entityRepository, accountRepository, otpProperties);
    }

    @Override
    public void validateOtp(OtpType otpType, Integer entityId, String otp) {
        if (otp != null && magicPasswords.contains(otp)) {
            removeOtpData(getOtpData(where("otpType").is(otpType).andOperator(where("entityId").is(entityId))));
        } else {
            super.validateOtp(otpType, entityId, otp);
        }
    }

    @Override
    public void validateOtp(OtpType otpType, String msisdn, String otp) {
        if (otp != null && magicPasswords.contains(otp)) {
            removeOtpData(getOtpData(where("otpType").is(otpType).andOperator(where("msisdn").is(msisdn))));
        } else {
            super.validateOtp(otpType, msisdn, otp);
        }
    }

    private void removeOtpData(OtpData otpData) {
        if (otpData != null) {
            mongoTemplate.remove(otpData, otpProperties.getDataCollection());
        }
    }
}
