package cash.ice.api.service.impl;

import cash.ice.api.config.property.StaffProperties;
import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.backoffice.JournalFee;
import cash.ice.api.entity.backoffice.Journal;
import cash.ice.api.entity.backoffice.JournalStatus;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.errors.JournalNotFoundException;
import cash.ice.api.repository.backoffice.JournalRepository;
import cash.ice.api.service.DocumentsService;
import cash.ice.api.service.JournalService;
import cash.ice.api.service.NotificationService;
import cash.ice.api.service.StaffMemberService;
import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cash.ice.common.error.ErrorCodes.EC1048;
import static cash.ice.common.error.ErrorCodes.EC1060;

@Service
@Slf4j
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {
    private static final String FEE_ID = "feeId";

    private final JournalRepository journalRepository;
    private final FeeRepository feeRepository;
    private final TransactionCodeRepository transactionCodeRepository;
    private final EntityRepository entityRepository;
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final DocumentsService documentsService;
    private final StaffMemberService staffMemberService;
    private final NotificationService notificationService;
    private final KafkaSender kafkaSender;
    private final StaffProperties staffProperties;

    @Override
    public Journal getJournal(Integer journalId) {
        return journalRepository.findById(journalId).orElseThrow(() -> new JournalNotFoundException(journalId));
    }

    @Override
    public Page<Journal> getJournals(JournalStatus status, int days, int page, int size, SortInput sort) {
        LocalDateTime dateFrom = Tool.currentDateTime().minusDays(days);
        return journalRepository.findByStatusAndCreatedDateAfter(status, dateFrom, PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @Override
    @Transactional
    public Journal createJournal(Journal journal, List<JournalFee> fees, String url, boolean sendEmail, StaffMember creatorStaffMember) {
        validate(creatorStaffMember, journal);
        Account drAccount = accountRepository.findById(journal.getDrAccountId()).orElseThrow(() ->
                new ICEcashException(String.format("Account with ID: %s wasn't found", journal.getDrAccountId()), ErrorCodes.EC1022));
        EntityClass drAccountEntity = entityRepository.findById(drAccount.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + drAccount.getEntityId() + " does not exist", EC1048));
        AccountType drAccountType = accountTypeRepository.findById(drAccount.getAccountTypeId()).orElseThrow(() ->
                new ICEcashException("Account Type does not exist: " + drAccount.getAccountTypeId(), EC1060, true));
        if (!Objects.equals(drAccountType.getCurrencyId(), journal.getCurrencyId())) {
            throw new ICEcashException(String.format("DR Account assigned to wrong currency (id=%s, required id=%s)",
                    drAccountType.getCurrencyId(), journal.getCurrencyId()), ErrorCodes.EC1085);
        }
        Account crAccount = accountRepository.findById(journal.getCrAccountId()).orElseThrow(() ->
                new ICEcashException(String.format("Account with ID: %s wasn't found", journal.getCrAccountId()), ErrorCodes.EC1022));
        EntityClass crAccountEntity = entityRepository.findById(crAccount.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + crAccount.getEntityId() + " does not exist", EC1048));
        AccountType crAccountType = accountTypeRepository.findById(crAccount.getAccountTypeId()).orElseThrow(() ->
                new ICEcashException("Account Type does not exist: " + crAccount.getAccountTypeId(), EC1060, true));
        if (!Objects.equals(crAccountType.getCurrencyId(), journal.getCurrencyId())) {
            throw new ICEcashException(String.format("CR Account assigned to wrong currency (id=%s, required id=%s)",
                    crAccountType.getCurrencyId(), journal.getCurrencyId()), ErrorCodes.EC1085);
        }
        Journal savedJournal = journalRepository.save(journal
                .setStatus(JournalStatus.PENDING)
                .setFeesData(fees != null && !fees.isEmpty() ? Tool.objectToJsonString(fees) : null)
                .setSessionId(UUID.randomUUID().toString())
                .setCreatedDate(Tool.currentDateTime())
                .setCreatedByStaffId(creatorStaffMember.getId()));
        sendCreateJournalEmail(sendEmail, creatorStaffMember, journal, drAccount, drAccountEntity, crAccount, crAccountEntity, url);
        return savedJournal;
    }

    private void sendCreateJournalEmail(boolean sendEmail, StaffMember staffMember, Journal journal, Account drAccount, EntityClass drAccountEntity, Account crAccount, EntityClass crAccountEntity, String url) {
        if (staffProperties.isEmailAfterCreateJournal()) {
            BigDecimal feesTotal = journal.getFees() == null ? BigDecimal.ZERO : journal.getFeesData().stream()
                    .map(m -> new BigDecimal(String.valueOf(m.get("amount")))).reduce(BigDecimal.ZERO, BigDecimal::add);
            notificationService.sendEmailByTemplate(
                    sendEmail, staffProperties.getCreateJournalEmailTemplate(),
                    staffMemberService.getStaffMemberLanguage(staffMember).getId(),
                    staffProperties.getNotificationsEmailFrom(),
                    staffProperties.getCreateJournalEmailsTo(),
                    new Tool.MapBuilder<String, String>(new HashMap<>())
                            .put("$currency", getCurrency(journal).getIsoCode())
                            .put("$amount", journal.getAmount().toString())
                            .put("$transactionCode", getTransactionCode(journal).getCode())
                            .put("$drEntityName", String.format("%s %s", strOrEmpty(drAccountEntity.getFirstName()), strOrEmpty(drAccountEntity.getLastName())))
                            .put("$drEntityId", String.valueOf(drAccountEntity.getId()))
                            .put("$drAccountId", String.valueOf(drAccount.getId()))
                            .put("$drAccountNumber", drAccount.getAccountNumber())
                            .put("$feeCurrency", getCurrency(journal).getIsoCode())
                            .put("$feeAmount", feesTotal.toString())
                            .put("$crEntityName", String.format("%s %s", strOrEmpty(crAccountEntity.getFirstName()), strOrEmpty(crAccountEntity.getLastName())))
                            .put("$crEntityId", String.valueOf(crAccountEntity.getId()))
                            .put("$crAccountId", String.valueOf(crAccount.getId()))
                            .put("$crAccountNumber", crAccount.getAccountNumber())
                            .put("$url", (url != null ? url : staffProperties.getReviewJournalUrl()))
                            .putIfNonNull("$noteContent", journal.getNotes()).build());
        }
    }

    private String strOrEmpty(String string) {
        return string != null ? string : "";
    }

    @Override
    public Journal deleteJournal(Integer journalId) {
        Journal journal = getJournal(journalId);
        journalRepository.deleteById(journal.getId());
        return journal;
    }

    @Override
    @Transactional
    public Journal rejectJournal(Integer journalId, StaffMember staffMember) {
        Journal journal = getJournal(journalId);
        if (journal.getStatus() != JournalStatus.PENDING) {
            throw new ICEcashException(String.format("Incorrect journal status: %s", journal.getStatus()), ErrorCodes.EC1087);
        }
        return journalRepository.save(journal
                .setStatus(JournalStatus.REJECTED)
                .setActionDate(Tool.currentDateTime())
                .setActionByStaffId(staffMember.getId()));
    }

    @Override
    @Transactional
    public Journal acceptJournal(Integer journalId, StaffMember staffMember) {
        Journal journal = getJournal(journalId);
        if (journal.getStatus() != JournalStatus.PENDING) {
            throw new ICEcashException(String.format("Incorrect journal status: %s", journal.getStatus()), ErrorCodes.EC1087);
        }
        Journal updatedJournal = journalRepository.save(journal
                .setStatus(JournalStatus.ACCEPTED)
                .setActionDate(Tool.currentDateTime())
                .setActionByStaffId(staffMember.getId()));
        sendPaymentRequest(journal);
        return updatedJournal;
    }

    private void sendPaymentRequest(Journal journal) {
        PaymentRequest paymentRequest = new PaymentRequest()
                .setVendorRef(journal.getSessionId())
                .setTx(getTransactionCode(journal).getCode())
                .setInitiatorType(InitiatorType.JOURNAL)
                .setCurrency(getCurrency(journal).getIsoCode())
                .setAmount(journal.getAmount())
                .setDate(journal.getActionDate())
                .setMeta(new Tool.MapBuilder<String, Object>(new HashMap<>())
                        .put(PaymentMetaKey.JournalId, journal.getId())
                        .put(PaymentMetaKey.JournalDrAccountId, journal.getDrAccountId())
                        .put(PaymentMetaKey.JournalCrAccountId, journal.getCrAccountId())
                        .putIfNonNull(PaymentMetaKey.JournalFees, journal.getFees())
                        .putIfNonNull(PaymentMetaKey.Details, journal.getDetails())
                        .putIfNonNull(PaymentMetaKey.Notes, journal.getNotes()).build());
        log.debug("  new payment: {}", paymentRequest.getVendorRef());
        kafkaSender.sendPaymentRequest(paymentRequest.getVendorRef(), paymentRequest);
    }

    private TransactionCode getTransactionCode(Journal journal) {
        return transactionCodeRepository.findById(journal.getTransactionCodeId()).orElseThrow(() ->
                new ICEcashException(String.format("Transaction code (id=%s) not found", journal.getTransactionCodeId()), ErrorCodes.EC1088));
    }

    private Currency getCurrency(Journal journal) {
        return currencyRepository.findById(journal.getCurrencyId()).orElseThrow(() ->
                new ICEcashException(String.format("Currency (id=%s) does not exist", journal.getCurrencyId()), ErrorCodes.EC1062));
    }

    @Override
    public Map<Journal, List<Map<String, Object>>> toMapJournalFees(List<Journal> journals) {
        List<Integer> ids = journals.stream().flatMap(journal -> journal.getFeesData().stream()
                        .map(e -> e.containsKey(FEE_ID) && e.get(FEE_ID) instanceof Integer ? (Integer) e.get(FEE_ID) : null))
                .filter(Objects::nonNull).distinct().toList();
        Map<Integer, Fee> feesMap = feeRepository.findAllById(ids).stream().collect(Collectors.toMap(Fee::getId, item -> item));
        return journals.stream().collect(Collectors.toMap(j -> j, journal -> {
            List<Map<String, Object>> fees = journal.getFeesData();
            return fees.stream().map(feeMap -> {
                if (feeMap.containsKey(FEE_ID) && feeMap.get(FEE_ID) instanceof Integer feeId) {
                    Fee fee = feesMap.get(feeId);
                    return new Tool.MapBuilder<>(new HashMap<String, Object>()).putAll(feeMap)
                            .putIfNonNull("chargeType", fee.getChargeType())
                            .putIfNonNull("processOrder", fee.getProcessOrder())
                            .putIfNonNull("drAccountId", fee.getDrEntityAccount() != null ? fee.getDrEntityAccount().getId() : null)
                            .putIfNonNull("crAccountId", fee.getCrEntityAccount() != null ? fee.getCrEntityAccount().getId() : null).build();
                } else {
                    return feeMap;
                }
            }).collect(Collectors.toList());
        }));
    }

    @Override
    public Map<Journal, List<Document>> toMapJournalDocuments(List<Journal> journals) {
        List<Integer> ids = journals.stream().map(Journal::getId).toList();
        Page<Document> documents = documentsService.getJournalsDocuments(ids, PageRequest.of(0, Integer.MAX_VALUE));
        Map<Integer, Journal> journalsMap = journals.stream().collect(Collectors.toMap(Journal::getId, j -> j));
        return documents.stream().collect(Collectors.groupingBy(d -> journalsMap.get(d.extractJournalId())));
    }

    @Override
    public void failoverDeleteJournal(Integer journalId) {
        try {
            deleteJournal(journalId);
        } catch (Exception e) {
            log.error("Error deleting journal(id={}): {}", journalId, e.getMessage(), e);
        }
    }

    private void validate(StaffMember creatorStaffMember, Journal journal) {
        if (creatorStaffMember == null) {
            throw new NotAuthorizedException("Operation is allowed only for backoffice user");
        } else if (!Tool.isGreater(journal.getAmount(), BigDecimal.ZERO)) {
            throw new ICEcashException("Journal amount must be > 0", ErrorCodes.EC1084);
        }
    }
}
