package cash.ice.api.service.impl;

import cash.ice.api.config.property.StaffProperties;
import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.backoffice.JournalFee;
import cash.ice.api.entity.backoffice.Journal;
import cash.ice.api.entity.backoffice.JournalStatus;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.repository.backoffice.JournalRepository;
import cash.ice.api.service.DocumentsService;
import cash.ice.api.service.NotificationService;
import cash.ice.api.service.StaffMemberService;
import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.service.KafkaSender;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cash.ice.sqldb.entity.Currency.MZN;
import static cash.ice.sqldb.entity.TransactionCode.TSF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceImplTest {
    private static final int JOURNAL_ID = 1;
    private static final int STAFF_MEMBER_ID = 2;
    private static final int CURRENCY_ID = 3;
    private static final int TRANSACTION_CODE_ID = 4;
    private static final int DR_ACCOUNT_ID = 5;
    private static final int CR_ACCOUNT_ID = 6;
    private static final int DR_ACCOUNT_TYPE_ID = 7;
    private static final int CR_ACCOUNT_TYPE_ID = 8;
    private static final String DETAILS = "details1";
    private static final String NOTES = "notes1";
    private static final String SESSION_ID = "sessionId1";

    @Mock
    private JournalRepository journalRepository;
    @Mock
    private FeeRepository feeRepository;
    @Mock
    private TransactionCodeRepository transactionCodeRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountTypeRepository accountTypeRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private DocumentsService documentsService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private StaffMemberService staffMemberService;
    @Mock
    private KafkaSender kafkaSender;
    @Mock
    private StaffProperties staffProperties;
    @Captor
    private ArgumentCaptor<PaymentRequest> paymentRequestCaptor;
    @InjectMocks
    private JournalServiceImpl service;

    @Test
    void testGetJournals() {
        int page = 0, size = 30, days = 30;
        when(journalRepository.findByStatusAndCreatedDateAfter(eq(JournalStatus.PENDING), any(), eq(PageRequest.of(page, size, SortInput.toSort(null)))))
                .thenReturn(new PageImpl<>(List.of(new Journal().setId(JOURNAL_ID), new Journal().setId(2))));
        Page<Journal> actualJournals = service.getJournals(JournalStatus.PENDING, days, page, size, null);
        assertThat(actualJournals.getContent().size()).isEqualTo(2);
        assertThat(actualJournals.getContent().get(0).getId()).isEqualTo(1);
        assertThat(actualJournals.getContent().get(1).getId()).isEqualTo(2);
    }

    @Test
    void testCreateJournals() {
        Journal journal = new Journal().setCurrencyId(CURRENCY_ID).setTransactionCodeId(TRANSACTION_CODE_ID).setAmount(BigDecimal.TEN)
                .setDrAccountId(DR_ACCOUNT_ID).setCrAccountId(CR_ACCOUNT_ID).setDetails(DETAILS).setNotes(NOTES);
        StaffMember authStaff = new StaffMember().setId(STAFF_MEMBER_ID);

        when(accountRepository.findById(DR_ACCOUNT_ID)).thenReturn(Optional.of(new Account().setAccountTypeId(DR_ACCOUNT_TYPE_ID).setEntityId(DR_ACCOUNT_ID)));
        when(entityRepository.findById(DR_ACCOUNT_ID)).thenReturn(Optional.of(new EntityClass().setFirstName("drFirstName")));
        when(accountRepository.findById(CR_ACCOUNT_ID)).thenReturn(Optional.of(new Account().setAccountTypeId(CR_ACCOUNT_TYPE_ID).setEntityId(CR_ACCOUNT_ID)));
        when(entityRepository.findById(CR_ACCOUNT_ID)).thenReturn(Optional.of(new EntityClass().setFirstName("crFirstName")));
        when(accountTypeRepository.findById(DR_ACCOUNT_TYPE_ID)).thenReturn(Optional.of(new AccountType().setCurrencyId(CURRENCY_ID)));
        when(accountTypeRepository.findById(CR_ACCOUNT_TYPE_ID)).thenReturn(Optional.of(new AccountType().setCurrencyId(CURRENCY_ID)));
        when(journalRepository.save(journal)).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(staffProperties.isEmailAfterCreateJournal()).thenReturn(true);
        when(staffProperties.getCreateJournalEmailTemplate()).thenReturn("create-journal-email-template");
        when(staffProperties.getNotificationsEmailFrom()).thenReturn("notification-email-from");
        when(staffProperties.getCreateJournalEmailsTo()).thenReturn(List.of("notification-emails-to"));
        when(staffMemberService.getStaffMemberLanguage(authStaff)).thenReturn(new Language().setId(11));
        when(currencyRepository.findById(CURRENCY_ID)).thenReturn(Optional.of(new Currency().setIsoCode(MZN)));
        when(transactionCodeRepository.findById(TRANSACTION_CODE_ID)).thenReturn(Optional.of(new TransactionCode().setCode(TSF)));

        Journal actualResponse = service.createJournal(journal, List.of(new JournalFee().setAmount(BigDecimal.ONE)), null, true, authStaff);
        assertThat(actualResponse.getStatus()).isEqualTo(JournalStatus.PENDING);
        assertThat(actualResponse.getCreatedDate()).isNotNull();
        assertThat(actualResponse.getCreatedByStaffId()).isEqualTo(STAFF_MEMBER_ID);
        verify(notificationService).sendEmailByTemplate(
                eq(true), eq("create-journal-email-template"),
                eq(11),
                eq("notification-email-from"),
                eq(List.of("notification-emails-to")),
                any());
    }

    @Test
    void testDeleteJournals() {
        when(journalRepository.findById(1)).thenReturn(Optional.of(new Journal().setId(JOURNAL_ID)));
        service.deleteJournal(JOURNAL_ID);
        verify(journalRepository).deleteById(JOURNAL_ID);
    }

    @Test
    void testRejectJournals() {
        Journal journal = new Journal().setStatus(JournalStatus.PENDING);

        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(journal));
        when(journalRepository.save(journal)).thenAnswer(invocation -> invocation.getArguments()[0]);

        Journal actualResponse = service.rejectJournal(JOURNAL_ID, new StaffMember().setId(STAFF_MEMBER_ID));
        assertThat(actualResponse.getStatus()).isEqualTo(JournalStatus.REJECTED);
        assertThat(actualResponse.getActionDate()).isNotNull();
        assertThat(actualResponse.getActionByStaffId()).isEqualTo(STAFF_MEMBER_ID);
    }

    @Test
    void testAcceptJournals() {
        Journal journal = new Journal().setId(JOURNAL_ID).setStatus(JournalStatus.PENDING).setAmount(BigDecimal.TEN).setDrAccountId(DR_ACCOUNT_ID).setCrAccountId(CR_ACCOUNT_ID)
                .setCurrencyId(CURRENCY_ID).setSessionId(SESSION_ID).setTransactionCodeId(TRANSACTION_CODE_ID).setDetails(DETAILS).setNotes(NOTES);

        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(journal));
        when(journalRepository.save(journal)).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(transactionCodeRepository.findById(TRANSACTION_CODE_ID)).thenReturn(Optional.of(new TransactionCode().setId(TRANSACTION_CODE_ID).setCode(TSF)));
        when(currencyRepository.findById(CURRENCY_ID)).thenReturn(Optional.of(new Currency().setId(CURRENCY_ID).setIsoCode(MZN)));

        Journal actualResponse = service.acceptJournal(JOURNAL_ID, new StaffMember().setId(STAFF_MEMBER_ID));
        assertThat(actualResponse.getStatus()).isEqualTo(JournalStatus.ACCEPTED);
        assertThat(actualResponse.getActionDate()).isNotNull();
        assertThat(actualResponse.getActionByStaffId()).isEqualTo(STAFF_MEMBER_ID);
        verify(kafkaSender).sendPaymentRequest(eq(SESSION_ID), paymentRequestCaptor.capture());
        PaymentRequest actualPaymentRequest = paymentRequestCaptor.getValue();
        assertThat(actualPaymentRequest.getVendorRef()).isEqualTo(SESSION_ID);
        assertThat(actualPaymentRequest.getTx()).isEqualTo(TSF);
        assertThat(actualPaymentRequest.getInitiatorType()).isEqualTo(InitiatorType.JOURNAL);
        assertThat(actualPaymentRequest.getCurrency()).isEqualTo(MZN);
        assertThat(actualPaymentRequest.getAmount()).isEqualTo(BigDecimal.TEN);
        assertThat(actualPaymentRequest.getMeta()).isEqualTo(Map.of(
                PaymentMetaKey.JournalId, JOURNAL_ID,
                PaymentMetaKey.JournalDrAccountId, DR_ACCOUNT_ID,
                PaymentMetaKey.JournalCrAccountId, CR_ACCOUNT_ID,
                PaymentMetaKey.Details, DETAILS,
                PaymentMetaKey.Notes, NOTES));
    }

    @Test
    void testToMapJournalFees() {
        List<Journal> request = List.of(new Journal().setFees(List.of(Map.of("feeId", 1), Map.of("feeId", 2))));

        when(feeRepository.findAllById(List.of(1, 2))).thenReturn(List.of(
                new Fee().setId(1).setChargeType(ChargeType.ORIGINAL).setProcessOrder(1).setDrEntityAccount(new Account().setId(11)).setCrEntityAccount(new Account().setId(12)),
                new Fee().setId(2).setChargeType(ChargeType.FIXED).setProcessOrder(2).setDrEntityAccount(new Account().setId(13)).setCrEntityAccount(new Account().setId(14))));

        Map<Journal, List<Map<String, Object>>> actualResponse = service.toMapJournalFees(request);
        assertThat(actualResponse.size()).isEqualTo(1);
        List<Map<String, Object>> maps = actualResponse.get(request.get(0));
        assertThat(maps).isEqualTo(List.of(
                Map.of("chargeType", ChargeType.ORIGINAL, "feeId", 1, "processOrder", 1, "drAccountId", 11, "crAccountId", 12),
                Map.of("chargeType", ChargeType.FIXED, "feeId", 2, "processOrder", 2, "drAccountId", 13, "crAccountId", 14)));
    }

    @Test
    void testToMapJournalDocuments() {
        List<Journal> journals = List.of(new Journal().setId(1), new Journal().setId(2));
        List<Document> documents = List.of(
                new Document().setId(11).placeJournalId(1),
                new Document().setId(12).placeJournalId(2),
                new Document().setId(13).placeJournalId(2));

        when(documentsService.getJournalsDocuments(List.of(1, 2), PageRequest.of(0, Integer.MAX_VALUE))).thenReturn(new PageImpl<>(documents));

        Map<Journal, List<Document>> actualResponse = service.toMapJournalDocuments(journals);
        assertThat(actualResponse.size()).isEqualTo(2);
        assertThat(actualResponse.get(journals.get(0))).isEqualTo(List.of(documents.get(0)));
        assertThat(actualResponse.get(journals.get(1))).isEqualTo(List.of(documents.get(1), documents.get(2)));
    }
}