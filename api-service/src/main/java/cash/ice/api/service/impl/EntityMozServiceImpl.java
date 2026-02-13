package cash.ice.api.service.impl;

import cash.ice.api.config.property.DeploymentConfigProperties;
import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.ConfigInput;
import cash.ice.api.dto.OtpType;
import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.moz.IdTypeMoz;
import cash.ice.api.dto.moz.LookupEntityType;
import cash.ice.api.dto.moz.MoneyProviderMoz;
import cash.ice.api.dto.moz.TagInfoMoz;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.EntityMozService;
import cash.ice.api.service.Me60MozService;
import cash.ice.api.service.TopUpServiceSelector;
import cash.ice.api.service.OtpService;
import cash.ice.api.service.PermissionsService;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.repository.*;
import cash.ice.sqldb.repository.moz.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.util.StringUtils;

import static cash.ice.common.error.ErrorCodes.*;
import static cash.ice.sqldb.entity.AccountType.*;

@Service
@Profile(IceCashProfile.PROD)
@RequiredArgsConstructor
@Slf4j
public class EntityMozServiceImpl implements EntityMozService {
    protected final EntityRepository entityRepository;
    private final PermissionsService permissionsService;
    private final OtpService otpService;
    private final Me60MozService me60MozService;
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final DeviceRepository deviceRepository;
    private final InitiatorRepository initiatorRepository;
    private final InitiatorStatusRepository initiatorStatusRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final MozProperties mozProperties;
    private final DeploymentConfigProperties deploymentConfigProperties;
    private final TopUpServiceSelector topUpServiceSelector;

    @Override
    public EntityClass getAuthEntity(AuthUser authUser, ConfigInput config) {
        return permissionsService.getAuthEntity(authUser);
    }

    @Override
    @PreAuthorize("@MozProperties.securityDisabled || isAuthenticated()")
    public EntityClass getEntityById(Integer id) {
        return entityRepository.findById(id).orElseThrow(() -> new UnexistingUserException("id: " + id));
    }

    @Override
    public Page<Initiator> getEntityInitiators(EntityClass entity, int page, int size, SortInput sort) {
        Currency mznCurrency = currencyRepository.findByIsoCode(Currency.MZN).orElseThrow(() ->
                new ICEcashException("'MZN' currency does not exist", EC1062));
        AccountType prepaidAccountType = accountTypeRepository.findByNameAndCurrencyId(PREPAID_TRANSPORT, mznCurrency.getId()).orElseThrow(() ->
                new ICEcashException("'Prepaid' account type for MZN does not exist", EC1060));
        Account account = accountRepository.findByEntityIdAndAccountTypeId(entity.getId(), prepaidAccountType.getId()).stream().findFirst().orElseThrow(() ->
                new ICEcashException("Account does not exist", EC1022));
        return initiatorRepository.findByAccountId(account.getId(), PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @Override
    public Page<Device> getEntityDevices(EntityClass entity, boolean linkedToVehicle, int page, int size, SortInput sort) {
        Currency currency = currencyRepository.findByIsoCode(Currency.MZN).orElseThrow(() ->
                new ICEcashException("MZN currency does not exist", EC1062));
        AccountType accountType = accountTypeRepository.findByNameAndCurrencyId(PRIMARY_ACCOUNT, currency.getId()).orElseThrow(() ->
                new ICEcashException(String.format("'Primary' account type for %s currency does not exist", currency.getId()), EC1060));
        Account account = accountRepository.findByEntityIdAndAccountTypeId(entity.getId(), accountType.getId()).stream().findFirst().orElseThrow(() ->
                new ICEcashException("Primary account does not exist", EC1022));
        if (linkedToVehicle) {
            return deviceRepository.findByAccountIdAndVehicleIdIsNotNull(account.getId(), PageRequest.of(page, size, SortInput.toSort(sort)));
        } else {
            return deviceRepository.findByAccountIdAndVehicleIdIsNull(account.getId(), PageRequest.of(page, size, SortInput.toSort(sort)));
        }
    }

    @Override
    public TagInfoMoz getTagInfo(String tagNumber) {
        Initiator initiator = initiatorRepository.findByIdentifier(tagNumber).orElseThrow(() ->
                new ICEcashException(String.format("Initiator: %s does not exist", tagNumber), ErrorCodes.EC1012));
        InitiatorStatus initiatorStatus = initiatorStatusRepository.findById(initiator.getInitiatorStatusId())
                .orElseThrow(() -> new ICEcashException(String.format("Initiator status with ID: '%s' does not exist", initiator.getInitiatorStatusId()), EC1059, true));
        TagInfoMoz response = new TagInfoMoz().setStatus(initiatorStatus.getName());
        if (initiator.getAccountId() != null) {
            Account account = accountRepository.findById(initiator.getAccountId()).orElseThrow(() ->
                    new ICEcashException(EC1022, String.format("Account with ID: '%s' does not exist", initiator.getAccountId())));
            EntityClass entity = entityRepository.findById(account.getEntityId())
                    .orElseThrow(() -> new ICEcashException("Entity with id=" + account.getEntityId() + " does not exist", EC1048));
            response.setAccountNumber(account.getAccountNumber())
                    .setFirstName(entity.getFirstName())
                    .setLastName(entity.getLastName())
                    .setPrepaidBalance(accountBalanceRepository.findByAccountId(account.getId()).map(AccountBalance::getBalance).orElse(BigDecimal.ZERO));

            Currency currency = currencyRepository.findByIsoCode(Currency.MZN).orElseThrow(() -> new ICEcashException("'MZN' currency does not exist", EC1062));
            AccountType subsidyAccountType = accountTypeRepository.findByNameAndCurrencyId(SUBSIDY_ACCOUNT, currency.getId()).orElseThrow(() ->
                    new ICEcashException(String.format("'%s' account type for %s currency does not exist", SUBSIDY_ACCOUNT, currency.getId()), EC1060));
            Account subsidyAccount = accountRepository.findByEntityIdAndAccountTypeId(entity.getId(), subsidyAccountType.getId())
                    .orElseThrow(() -> new ICEcashException("Subsidy account is absent", ErrorCodes.EC3026, true));
            response.setSubsidyBalance(accountBalanceRepository.findByAccountId(subsidyAccount.getId())
                    .map(AccountBalance::getBalance).orElse(BigDecimal.ZERO));
        }
        return response;
    }

    public Account getAccount(EntityClass entity, String accountType, String currencyCode) {
        Currency mznCurrency = currencyRepository.findByIsoCode(currencyCode).orElseThrow(() ->
                new ICEcashException("'MZN' currency does not exist", EC1062));
        AccountType primaryAccountType = accountTypeRepository.findByNameAndCurrencyId(accountType, mznCurrency.getId())
                .orElseThrow(() -> new ICEcashException("Primary account type does not exist", EC1063));
        return accountRepository.findByEntityIdAndAccountTypeId(entity.getId(), primaryAccountType.getId()).stream().findFirst().orElseThrow(() ->
                new ICEcashException("Primary Account does not exist", EC1022));
    }

    @Override
    public List<EntityClass> lookupEntity(LookupEntityType lookupBy, IdTypeMoz idType, String value) {
        if (lookupBy == LookupEntityType.ID && idType == null) {
            throw new ICEcashException("ID type is not provided", ErrorCodes.EC1075);
        }
        List<EntityClass> entities = switch (lookupBy) {
            case MSISDN -> entityMsisdnRepository.findByMsisdn(value).stream().map(EntityMsisdn::getEntityId).distinct()
                    .map(this::getEntityById).toList();
            case ID -> entityRepository.findByIdNumberAndIdType(value, idType.getDbId());
            case ACCOUNT -> accountRepository.findByAccountNumber(value).stream().map(Account::getEntityId).distinct()
                    .map(this::getEntityById).toList();
            default -> throw new ICEcashException("Unknown lookupBy criteria: " + lookupBy, ErrorCodes.EC1047);
        };
        return entities.stream().filter(entity -> entity.getStatus() == EntityStatus.ACTIVE).toList();
    }

    @Override
    @Transactional
    public EntityClass addOrUpdateMsisdn(EntityClass entity, MsisdnType type, String mobile, String oldMobile, String description, String otp) {
        if (mozProperties.isLinkTagCheckOtp()) {
            otpService.validateOtp(OtpType.MOZ_MSISDN_UPDATE, mobile, otp);
        }
        EntityMsisdn msisdn;
        if (type == MsisdnType.PRIMARY) {
            msisdn = entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(entity.getId()).orElse(null);
        } else {
            msisdn = entityMsisdnRepository.findByEntityIdAndMsisdnType(entity.getId(), type).stream()
                    .filter(entityMsisdn -> Objects.equals(entityMsisdn.getMsisdn(), oldMobile)).findAny().orElse(null);
        }
        if (msisdn == null) {
            msisdn = new EntityMsisdn().setEntityId(entity.getId()).setMsisdnType(type).setCreatedDate(Tool.currentDateTime());
        }
        entityMsisdnRepository.save(msisdn
                .setMsisdn(mobile)
                .setDescription(description));
        return entity;
    }

    @Override
    public PaymentResponse topupAccount(EntityClass authEntity, String accountNumber, MoneyProviderMoz provider, String mobile, BigDecimal amount) {
        String countryCode = StringUtils.hasText(deploymentConfigProperties.getCountryCode())
                ? deploymentConfigProperties.getCountryCode().trim()
                : "KE";
        List<String> allowedProviders = topUpServiceSelector.getAllowedProviderIds(countryCode);
        if (!allowedProviders.contains(provider.name())) {
            throw new ICEcashException(
                    String.format("Top-up provider %s is not available for country %s", provider.name(), countryCode),
                    EC1001);
        }
        Account account = accountRepository.findByAccountNumber(accountNumber).stream().findFirst().orElseThrow(() ->
                new ICEcashException(EC1022, String.format("Account %s does not exist", accountNumber)));
        EntityClass entity = entityRepository.findById(account.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + account.getEntityId() + " does not exist", EC1048));
        if (!Objects.equals(entity.getId(), authEntity.getId())) {
            throw new ICEcashException(String.format("Account %s does not belong to the user", accountNumber), EC1082);
        }
        return me60MozService.makePayment(new PaymentRequest()
                .setVendorRef(Tool.generateCharacters(20))
                .setTx(provider.getInboundTx())
                .setInitiatorType(provider.getInitiatorType())
                .setInitiator(mobile)
                .setCurrency(Currency.MZN)
                .setAmount(amount)
                .setDate(Tool.currentDateTime())
                .setMeta(Tool.newMetaMap()
                        .putIfNonNull("simulate", mozProperties.isSimulateTopupAccount() ? "SUCCESS" : null)
                        .put(PaymentMetaKey.AccountNumber, accountNumber)
                        .build()));
    }
}
