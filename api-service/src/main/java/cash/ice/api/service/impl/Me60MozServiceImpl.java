package cash.ice.api.service.impl;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.moz.*;
import cash.ice.api.errors.Me60Exception;
import cash.ice.api.errors.RegistrationException;
import cash.ice.api.service.*;
import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.*;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.entity.moz.DeviceStatus;
import cash.ice.sqldb.repository.*;
import cash.ice.sqldb.repository.moz.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static cash.ice.api.util.MappingUtil.accountBalancesMap;
import static cash.ice.api.util.MappingUtil.itemsToCategoriesMap;
import static cash.ice.common.error.ErrorCodes.*;
import static cash.ice.common.utils.Tool.moneyRound;
import static cash.ice.sqldb.entity.AccountType.PREPAID_TRANSPORT;
import static cash.ice.sqldb.entity.AccountType.SUBSIDY_ACCOUNT;
import static cash.ice.sqldb.entity.InitiatorType.TAG;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@RequiredArgsConstructor
@Slf4j
public class Me60MozServiceImpl implements Me60MozService {
    private static final String TRANSPORT_CATEGORY = "MZ Transport";
    private static final String ACTIVE = "Active";
    private static final String UNASSIGNED = "Unassigned";
    private static final String MT = "MT";
    private static final int TIMESTAMP_CHARS = 10;

    private final DeviceRepository deviceRepository;
    private final AccountRepository accountRepository;
    private final EntityRepository entityRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final InitiatorRepository initiatorRepository;
    private final InitiatorTypeRepository initiatorTypeRepository;
    private final InitiatorStatusRepository initiatorStatusRepository;
    private final InitiatorCategoryRepository initiatorCategoryRepository;
    private final CurrencyRepository currencyRepository;
    private final SecurityPvvService securityPvvService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final MongoTemplate mongoTemplate;
    private final LoggerService loggerService;
    private final MozProperties mozProperties;

    @Override
    public String registerDevice(MozAutoRegisterDeviceRequest request) {
        Device device = deviceRepository.findBySerial(request.getSerialNumber()).orElse(null);
        if (device == null) {
            device = deviceRepository.save(new Device()
                    .setCode(generateDeviceCode())
                    .setSerial(request.getSerialNumber())
                    .setMeta(request.getMetaData() != null && !request.getMetaData().isEmpty() ? request.getMetaData() :
                            Tool.newMetaMap()
                                    .putIfNonNull("productNumber", request.getProductNumber())
                                    .putIfNonNull("model", request.getModel())
                                    .putIfNonNull("bootVersion", request.getBootVersion())
                                    .putIfNonNull("cpuType", request.getCpuType())
                                    .putIfNonNull("rfidVersion", request.getRfidVersion())
                                    .putIfNonNull("osVersion", request.getOsVersion())
                                    .putIfNonNull("imei", request.getImei())
                                    .putIfNonNull("imsi", request.getImsi()).build())
                    .setAccountId(null)
                    .setStatus(DeviceStatus.INACTIVE)
                    .setCreatedDate(Tool.currentDateTime())
                    .setModifiedDate(Tool.currentDateTime()));
        }
        return device.getCode();
    }

    private String generateDeviceCode() {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String code = Tool.generateCharacters(mozProperties.getDeviceCodeCharacters());
            if (!deviceRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new RegistrationException(EC1009, "Cannot generate unique device code");
    }

    @Override
    public String linkTag(LinkNfcTagRequest request) {
        validateDeviceForLinking(request.getDeviceSerial());
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber()).stream().findFirst()
                .orElseThrow(() -> new Me60Exception("Invalid account", request.getAccountNumber(), EC1022));
        EntityClass entity = entityRepository.findById(account.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + account.getEntityId() + " does not exist", EC1048));
        EntityMsisdn msisdn = entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(entity.getId())
                .orElseThrow(() -> new Me60Exception("Invalid mobile number", "entity: " + entity.getId(), EC1046));
        InitiatorType initiatorType = initiatorTypeRepository.findByDescription(TAG)
                .orElseThrow(() -> new ICEcashException("'tag' initiator type does not exist", EC1057, true));
        InitiatorCategory initiatorCategory = initiatorCategoryRepository.findByCategory(TRANSPORT_CATEGORY)
                .orElseThrow(() -> new ICEcashException("'MZ Transport' initiator category does not exist", EC1058, true));
        InitiatorStatus activeStatus = initiatorStatusRepository.findByName(ACTIVE)
                .orElseThrow(() -> new ICEcashException("'Active' initiator status does not exist", EC1059, true));
        InitiatorStatus unassignedStatus = initiatorStatusRepository.findByName(UNASSIGNED)
                .orElseThrow(() -> new ICEcashException("'Unassigned' initiator status does not exist", EC1059, true));
        AccountType accountType = accountTypeRepository.findById(account.getAccountTypeId()).orElse(null);
        if (accountType == null || !PREPAID_TRANSPORT.equals(accountType.getName())) {
            throw new Me60Exception("Wrong account type", "Only 'Prepaid' account can be linked", EC1068);
        }
        AccountType subsidyAccountType = accountTypeRepository.findByNameAndCurrencyId(AccountType.SUBSIDY_ACCOUNT, accountType.getCurrencyId())
                .orElseThrow(() -> new ICEcashException("Subsidy account type does not exist", EC1063));
        Account subsidyAccount = accountRepository.findByEntityIdAndAccountTypeId(entity.getId(), subsidyAccountType.getId()).orElse(null);

        String otpKey = Tool.generateDigits(16, true);
        String otp = Tool.generateDigits(mozProperties.getLinkTagOtpDigitsAmount(), false);
        MozLinkTagData mozLinkTagData = mongoTemplate.save(new MozLinkTagData()
                        .setRequestId(UUID.randomUUID().toString())
                        .setDevice(request.getDeviceSerial())
                        .setAccountNumber(request.getAccountNumber())
                        .setAccountId(account.getId())
                        .setSubsidyAccountId(subsidyAccount != null ? subsidyAccount.getId() : null)
                        .setOtpKey(otpKey)
                        .setOtpPvv(securityPvvService.acquirePvv(otpKey, otp))
                        .setInitiatorTypeId(initiatorType.getId())
                        .setInitiatorCategoryId(initiatorCategory.getId())
                        .setActiveInitiatorStatusId(activeStatus.getId())
                        .setUnassignedInitiatorStatusId(unassignedStatus.getId())
                        .setFirstName(entity.getFirstName())
                        .setLastName(entity.getLastName())
                        .setCreatedDate(Instant.now()),
                mozProperties.getLinkTagDataCollection());
        notificationService.sendSmsMessage(otp, msisdn.getMsisdn());
        return mozLinkTagData.getRequestId();
    }

    private void validateDeviceForLinking(String deviceSerialOrCode) {
        if (mozProperties.isLinkTagValidateDevice()) {
            Device device = deviceRepository.findBySerial(deviceSerialOrCode).orElseGet(
                    () -> deviceRepository.findByCode(deviceSerialOrCode).orElse(null));
            if (device == null) {
                throw new Me60Exception("Device is not registered", deviceSerialOrCode, EC1055);
            } else if (device.getStatus() != DeviceStatus.ACTIVE) {
                throw new Me60Exception("Device is inactive", deviceSerialOrCode, EC1055);
            }
        }
    }

    @Override
    public TagLinkResponse linkTagValidateOtp(LinkNfcTagRequest request) {
        MozLinkTagData mozLinkTagData = getLinkTagData(request.getRequestId());
        try {
            if (request.getOtp() == null ||
                    !request.getOtp().matches("^[0-9]+$") ||
                    !Objects.equals(securityPvvService.acquirePvv(
                                    mozLinkTagData.getOtpKey(),
                                    request.getOtp()),
                            mozLinkTagData.getOtpPvv())) {
                throw new Me60Exception("Invalid password", request.getOtp(), EC1052);
            }
            TagLinkResponse accountBalances = getAccountBalances(mozLinkTagData.getAccountId(), mozLinkTagData.getSubsidyAccountId());
            mongoTemplate.save(mozLinkTagData.setOtpValidated(true));
            return accountBalances
                    .setFirstName(mozLinkTagData.getFirstName())
                    .setLastName(mozLinkTagData.getLastName());

        } catch (Exception e) {
            mongoTemplate.remove(mozLinkTagData, mozProperties.getLinkTagDataCollection());
            throw e;
        }
    }

    @Override
    public Initiator linkTagRegister(LinkNfcTagRequest request) {
        MozLinkTagData mozLinkTagData = getLinkTagData(request.getRequestId());
        try {
            if (!mozLinkTagData.isOtpValidated()) {
                throw new Me60Exception("Password is not validated", EC1053);
            }
            Initiator tag = initiatorRepository.findByIdentifier(request.getTagNumber()).orElseThrow(() ->
                    new Me60Exception("Invalid tag", String.format("Tag '%s' does not exist", request.getTagNumber()), EC1066));
            if (Objects.equals(tag.getInitiatorStatusId(), mozLinkTagData.getActiveInitiatorStatusId())) {
                throw new Me60Exception("Tag already linked", request.getTagNumber(), EC1064);
            } else if (!Objects.equals(tag.getInitiatorStatusId(), mozLinkTagData.getUnassignedInitiatorStatusId())) {
                throw new Me60Exception("Invalid status", String.format("Tag '%s' is not in 'Unassigned' status", request.getTagNumber()), EC1067);
            }
            return initiatorRepository.save(tag
                    .setIdentifier(request.getTagNumber())
                    .setInitiatorTypeId(mozLinkTagData.getInitiatorTypeId())
                    .setAccountId(mozLinkTagData.getAccountId())
                    .setInitiatorCategoryId(mozLinkTagData.getInitiatorCategoryId())
                    .setInitiatorStatusId(mozLinkTagData.getActiveInitiatorStatusId())
                    .setCreatedDate(Tool.currentDateTime())
                    .setStartDate(LocalDate.now()));
        } finally {
            mongoTemplate.remove(mozLinkTagData, mozProperties.getLinkTagDataCollection());
        }
    }

    private TagLinkResponse getAccountBalances(int accountId, Integer subsidyAccountId) {
        List<Integer> accounts = subsidyAccountId != null ? List.of(accountId, subsidyAccountId) : List.of(accountId);
        List<AccountBalance> balances = accountBalanceRepository.findByAccountIdIn(accounts);
        return new TagLinkResponse()
                .setPrepaidBalance(balances.stream().filter(balance -> balance.getAccountId() == accountId).findAny()
                        .map(AccountBalance::getBalance).orElse(BigDecimal.ZERO))
                .setSubsidyBalance(subsidyAccountId == null ? null : balances.stream().filter(balance -> Objects.equals(balance.getAccountId(), subsidyAccountId))
                        .findAny().map(AccountBalance::getBalance).orElse(BigDecimal.ZERO));
    }

    private MozLinkTagData getLinkTagData(String requestId) {
        Query query = query(where("requestId").is(requestId));
        MozLinkTagData data = mongoTemplate.findOne(query, MozLinkTagData.class, mozProperties.getLinkTagDataCollection());
        if (data == null) {
            throw new Me60Exception("Invalid request ID", EC1054);
        }
        return data;
    }

    @Override
    public void cleanupExpiredMozLinkTagTask() {
        Instant threshold = Instant.now().minus(mozProperties.getLinkTagRequestExpirationDuration());
        List<MozLinkTagData> expiredData = mongoTemplate.findAll(MozLinkTagData.class).stream()
                .filter(d -> d.getCreatedDate() == null || d.getCreatedDate().isBefore(threshold)).toList();
        if (!expiredData.isEmpty()) {
            log.info("Cleanup {} expired moz link tag requests older than: {}", expiredData.size(), threshold);
            List<String> ids = expiredData.stream().map(MozLinkTagData::getId).toList();
            mongoTemplate.remove(query(where("_id").in(ids)), mozProperties.getLinkTagDataCollection());
        }
    }

    @Override
    public MozAccountInfoResponse getAccountInfo(String deviceSerialOrCode) {
        Device device = getAndValidateDevice(deviceSerialOrCode);
        Account account = accountRepository.findById(device.getAccountId()).orElseThrow(() ->
                new Me60Exception("Linked invalid account", EC1022));
        EntityClass entity = entityRepository.findById(account.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + account.getEntityId() + " does not exist", EC1048));
        AccountType accountType = accountTypeRepository.findById(account.getAccountTypeId()).orElseThrow(() ->
                new ICEcashException("Account Type does not exist: " + account.getAccountTypeId(), EC1060, true));
        return new MozAccountInfoResponse()
                .setAccountId(account.getId())
                .setAccountNumber(account.getAccountNumber())
                .setAccountType(accountType.getDescription())
                .setFirstName(entity.getFirstName())
                .setLastName(entity.getLastName())
                .setDeviceCode(device.getCode())
                .setDeviceStatus(device.getStatus().toString())
                .setVehicleId(device.getVehicleId());
    }

    private Device getAndValidateDevice(String deviceSerialOrCode) {
        Device device = deviceRepository.findBySerial(deviceSerialOrCode).orElseGet(
                () -> deviceRepository.findByCode(deviceSerialOrCode).orElse(null));
        if (device == null) {
            throw new Me60Exception("Device is not registered", deviceSerialOrCode, EC1055);
        } else if (device.getStatus() != DeviceStatus.ACTIVE) {
            throw new Me60Exception("Device is inactive", deviceSerialOrCode, EC1055);
        } else if (device.getAccountId() == null) {
            throw new Me60Exception("No account linked to device", deviceSerialOrCode, EC1055);
        }
        return device;
    }

    @Override
    public PaymentResponse makePayment(PaymentRequest paymentRequest) {
        if (TransactionCode.TSF.equals(paymentRequest.getTx())) {
            PaymentRequest oldRequest = loggerService.getRequest(paymentRequest.getVendorRef(), PaymentRequest.class);
            if (oldRequest != null && Tool.isGreater(oldRequest.getAmount(), paymentRequest.getAmount())) {
                log.info("  found existing '{}' transaction with greater amount: {} -> {}, prepare it to rollback",
                        paymentRequest.getVendorRef(), oldRequest.getAmount(), paymentRequest.getAmount());
                paymentRequest.addMetaData(PaymentMetaKey.RollbackExistingTransaction, true);
                loggerService.removePayment(paymentRequest.getVendorRef());
            }
        }
        return paymentService.makePaymentSynchronous(paymentRequest,
                mozProperties.getPaymentTimeoutDuration(), this::postPaymentAction);
    }

    @Override
    public List<PaymentResponse> makeBulkPayment(List<PaymentRequest> paymentRequestList) {
        return paymentService.makeBulkPaymentSynchronous(paymentRequestList, false,
                mozProperties.getPaymentTimeoutDuration(), this::postPaymentAction);
    }

    @Override
    public PaymentResponse makeOffloadPayment(OffloadPaymentRequestMoz offloadPaymentRequest) {
        List<PaymentRequest> paymentRequestList = offloadPaymentRequest.getOffloadTransactions().stream().map(offloadTransaction -> {
            // offloadTransaction: [ DeviceCode(4 chars) + Timestamp(10 chars) + Amount(1-8 chars) ]
            int deviceCharsAmount = mozProperties.getDeviceCodeCharacters();
            if (offloadTransaction.length() < deviceCharsAmount + TIMESTAMP_CHARS + 1) {
                throw new Me60Exception("Wrong transaction: " + offloadTransaction, offloadTransaction, EC1055);
            }
            long cents = Long.parseLong(offloadTransaction.substring(deviceCharsAmount + TIMESTAMP_CHARS));
            BigDecimal amount = BigDecimal.valueOf(cents, 2);
            long timestamp = Long.parseLong(offloadTransaction, deviceCharsAmount, deviceCharsAmount + TIMESTAMP_CHARS, 10);
            LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), Tool.getZimZoneId());
            log.debug("  forming offload transaction for {}, cents: {}, amount: {}, initiator: {}, timestamp: {}, date: {}, deviceCode: {}",
                    offloadTransaction, cents, amount, offloadPaymentRequest.getTag(), timestamp, date, offloadTransaction.substring(0, deviceCharsAmount));
            return new PaymentRequest()
                    .setVendorRef(offloadTransaction)
                    .setTx(TransactionCode.TSF)
                    .setInitiatorType(TAG)
                    .setInitiator(offloadPaymentRequest.getTag())
                    .setCurrency(Currency.MZN)
                    .setAmount(amount)
                    .setDate(date)
                    .setApiVersion("1")
                    .setMeta(Map.of("deviceCode", offloadTransaction.substring(0, deviceCharsAmount)));
        }).toList();
        return makeBulkPayments(paymentRequestList);
    }

    @Override
    public PaymentResponse makeBulkPayments(List<PaymentRequest> paymentRequestList) {
        List<PaymentResponse> paymentResponses = paymentService.makeBulkPaymentSynchronous(paymentRequestList, true,
                mozProperties.getPaymentTimeoutDuration(), this::postPaymentAction);
        PaymentRequest lastRequest = paymentRequestList.getLast();
        PaymentResponse lastResponse = paymentResponses.getLast();
        return failoverFillPaymentBalancesIfNeed(PaymentResponseMoz.success(lastResponse),
                (lastResponse instanceof PaymentResponseMoz lastMoz ? lastMoz.getInitiatorEntityId() : null), lastRequest.getInitiator());
    }

    private PaymentResponse failoverFillPaymentBalancesIfNeed(PaymentResponseMoz paymentResponse, Integer initiatorEntityId, String tagName) {
        if (paymentResponse.getBalance() == null) {
            try {
                if (initiatorEntityId == null) {
                    InitiatorType initiatorType = initiatorTypeRepository.findByDescription(TAG).orElseThrow(() ->
                            new ICEcashException("'tag' initiator type does not exist", EC1057));
                    Initiator initiator = initiatorRepository.findByIdentifierAndInitiatorTypeId(tagName, initiatorType.getId()).orElseThrow(() ->
                            new ICEcashException(String.format("initiator does not exist, typeId=%s, identifier=%s", initiatorType.getId(), tagName), EC1012));
                    Account prepaidAccount = accountRepository.findById(initiator.getAccountId()).orElseThrow(() ->
                            new ICEcashException(String.format("Invalid account for initiator (id=%s)", initiator.getAccountId()), EC1022));
                    EntityClass prepaidAccountEntity = entityRepository.findById(prepaidAccount.getEntityId())
                            .orElseThrow(() -> new ICEcashException("Entity with id=" + prepaidAccount.getEntityId() + " does not exist", EC1048));
                    initiatorEntityId = prepaidAccountEntity.getId();
                }
                Map<String, PaymentResponseMoz.BalanceResponse> accountsBalances = getAccountsBalances(initiatorEntityId, List.of());
                log.debug("  get balances (failover): {}, initiatorEntityId: {}", accountsBalances, initiatorEntityId);
                paymentResponse.setBalance(getBalanceFor(PREPAID_TRANSPORT, accountsBalances.values()));
                paymentResponse.setSubsidyBalance(getBalanceFor(SUBSIDY_ACCOUNT, accountsBalances.values()));
            } catch (ICEcashException e) {
                log.warn(e.getMessage());
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
        return paymentResponse;
    }

    private BigDecimal getBalanceFor(String accountTypeName, Collection<PaymentResponseMoz.BalanceResponse> accountsBalances) {
        return accountsBalances.stream().filter(response -> accountTypeName.equals(response.getAccountType()))
                .findAny().map(PaymentResponseMoz.BalanceResponse::getBalance).orElse(null);
    }

    private void postPaymentAction(PaymentRequest paymentRequest, PaymentResponse paymentResponse) {
        if (paymentResponse.getStatus() == ResponseStatus.SUCCESS) {
            BigDecimal subsidyBalance = paymentResponse instanceof PaymentResponseMoz r ? r.getSubsidyBalance() : null;
            if (mozProperties.isPaymentConfirmationSmsEnable() && paymentResponse.getPrimaryMsisdn() != null) {
                notificationService.sendSmsMessage(String.format("pt".equals(paymentResponse.getLocale()) ?
                                mozProperties.getPaymentConfirmationSmsMessagePt() : mozProperties.getPaymentConfirmationSmsMessageEn(),
                        paymentResponse.getTransactionId(), paymentResponse.getDate() != null ? DateTimeFormatter.ofPattern("dd/MM HH:mm").format(paymentResponse.getDate()) : "",
                        moneyRound(paymentRequest.getAmount()) + " " + MT,
                        moneyRound(paymentResponse.getBalance()) + " " + MT,
                        moneyRound(subsidyBalance) + " " + MT), paymentResponse.getPrimaryMsisdn());
            } else {
                log.warn("Cannot send SMS, msisdn is null, request: {}, response: {}", paymentRequest, paymentResponse);
            }
        }
    }

    @Override
    public Map<String, PaymentResponseMoz.BalanceResponse> getAccountsBalances(Integer entityId, List<Integer> balanceAccountTypes) {
        if (balanceAccountTypes != null) {
            List<Account> accounts = accountRepository.findByEntityId(entityId).stream()
                    .filter(account -> balanceAccountTypes.isEmpty() || balanceAccountTypes.contains(account.getAccountTypeId())).toList();
            Map<Account, AccountType> accountTypeMap = itemsToCategoriesMap(accounts, Account::getAccountTypeId, AccountType::getId, accountTypeRepository);
            Map<AccountType, Currency> currencyMap = itemsToCategoriesMap(accountTypeMap.values().stream().distinct().toList(), AccountType::getCurrencyId, Currency::getId, currencyRepository);
            Map<Account, BigDecimal> balanceMap = accountBalancesMap(accounts, accountBalanceRepository::findByAccountIdIn, () -> BigDecimal.ZERO);
            HashMap<String, PaymentResponseMoz.BalanceResponse> result = new HashMap<>();
            accounts.forEach(account -> result.put(account.getAccountNumber(), new PaymentResponseMoz.BalanceResponse()
                    .setAccountTypeId(account.getAccountTypeId())
                    .setAccountType(accountTypeMap.get(account).getName())
                    .setCurrencyCode(currencyMap.get(accountTypeMap.get(account)).getIsoCode())
                    .setBalance(balanceMap.get(account))));
            return result;
        }
        return null;
    }

    @Override
    public String getOtp(String requestId) {
        MozLinkTagData mozLinkTagData = getLinkTagData(requestId);
        return securityPvvService.restorePin(mozLinkTagData.getOtpKey(), mozLinkTagData.getOtpPvv(), mozProperties.getLinkTagOtpDigitsAmount());
    }

    @Override
    public Initiator addClearTag(String tag) {
        Initiator initiator = initiatorRepository.findByIdentifier(tag).orElse(new Initiator().setIdentifier(tag));
        return initiatorRepository.save(initiator.setInitiatorTypeId(initiatorTypeRepository.findByDescription(TAG).map(InitiatorType::getId).orElseThrow())
                .setInitiatorCategoryId(initiatorCategoryRepository.findByCategory("MZ Transport").map(InitiatorCategory::getId).orElseThrow())
                .setInitiatorStatusId(initiatorStatusRepository.findByName("Unassigned").map(InitiatorStatus::getId).orElseThrow())
                .setCreatedDate(Tool.currentDateTime())
                .setAccountId(null)
                .setStartDate(null)
                .setExpiryDate(null));
    }

    @Override
    public Initiator removeTag(String tag) {
        Initiator initiator = initiatorRepository.findByIdentifier(tag).orElseThrow();
        initiatorRepository.delete(initiator);
        return initiator;
    }

    @Override
    public Device activateDevice(String serialOrCode, String accountNumber) {
        Device device = findDevice(serialOrCode);
        List<Account> accountNumbers = accountRepository.findByAccountNumber(accountNumber);
        if (accountNumbers.size() != 1) {
            throw new Me60Exception("Wrong account number", ErrorCodes.EC1013);
        }
        return deviceRepository.save(device
                .setAccountId(accountNumbers.get(0).getId())
                .setStatus(DeviceStatus.ACTIVE));
    }

    @Override
    public Device removeDevice(String serialOrCode) {
        Device device = findDevice(serialOrCode);
        deviceRepository.delete(device);
        return device;
    }

    private Device findDevice(String serialOrCode) {
        Device device = deviceRepository.findBySerial(serialOrCode).orElse(null);
        if (device == null) {
            device = deviceRepository.findByCode(serialOrCode).orElseThrow(() -> new ICEcashException("Wrong device", EC1055));
        }
        return device;
    }
}
