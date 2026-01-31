package cash.ice.api.service.impl;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentLineView;
import cash.ice.api.dto.PaymentView;
import cash.ice.api.entity.zim.Payment;
import cash.ice.api.entity.zim.PaymentStatus;
import cash.ice.api.parser.PaymentsBulkParserFactory;
import cash.ice.api.repository.zim.PaymentCollectionRepository;
import cash.ice.api.repository.zim.PaymentRepository;
import cash.ice.api.service.PaymentExecutionService;
import cash.ice.api.service.PermissionsService;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AuthorisationType;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.PaymentLine;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.PaymentLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cash.ice.api.entity.zim.PaymentStatus.PENDING;
import static cash.ice.api.entity.zim.PaymentStatus.PROCESSING;
import static cash.ice.api.service.impl.PendingPaymentServiceImpl.APPROVED2_ID;
import static cash.ice.api.service.impl.PendingPaymentServiceImpl.APPROVED_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingPaymentServiceImplTest {
    private static final int ACCOUNT_ID = 11;
    private static final int PAYMENT_ID = 17;
    private static final int AUTH_ENTITY_ID = 18;
    private static final int PREV_APPROVER = 13;
    private static final String REALM = "ONLINE";

    @Mock
    private PermissionsService permissionsService;
    @Mock
    private PaymentExecutionService paymentExecutionService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentLineRepository paymentLineRepository;
    @Mock
    private PaymentCollectionRepository paymentCollectionRepository;
    @Mock
    private PaymentsBulkParserFactory paymentsBulkParserFactory;
    @Mock
    private AccountRepository accountRepository;

    private PendingPaymentServiceImpl service;

    @BeforeEach
    void init() {
        service = new PendingPaymentServiceImpl(permissionsService, paymentExecutionService, paymentRepository,
                paymentLineRepository, paymentCollectionRepository, paymentsBulkParserFactory, accountRepository);
    }

    @Test
    void testGetPayments() {
        AuthUser authUser = new AuthUser();
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(
                new Payment().setId(ACCOUNT_ID).setAccountId(1),
                new Payment().setId(12).setAccountId(2)));
        when(paymentRepository.findAll(pageRequest)).thenReturn(page);
        when(permissionsService.checkReadRights(authUser, 1)).thenReturn(true);
        when(permissionsService.checkReadRights(authUser, 2)).thenReturn(false);
        when(paymentLineRepository.findByPaymentId(ACCOUNT_ID)).thenReturn(List.of(new PaymentLine().setId(21)));

        Page<PaymentView> actualResult = service.getPayments(pageRequest, authUser);
        assertThat(actualResult.getTotalElements()).isEqualTo(1);
        PaymentView paymentView = actualResult.getContent().get(0);
        assertThat(paymentView.getId()).isEqualTo(ACCOUNT_ID);
        assertThat(paymentView.getPaymentLines().get(0).getId()).isEqualTo(21);
    }

    @Test
    void testGetPayment() {
        AuthUser authUser = new AuthUser();
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(
                new Payment().setId(PAYMENT_ID).setAccountId(ACCOUNT_ID)));
        when(permissionsService.checkReadRights(authUser, ACCOUNT_ID)).thenReturn(true);
        when(paymentLineRepository.findByPaymentId(PAYMENT_ID)).thenReturn(List.of(new PaymentLine().setId(21)));

        PaymentView paymentView = service.getPayment(PAYMENT_ID, authUser);
        assertThat(paymentView.getId()).isEqualTo(PAYMENT_ID);
        assertThat(paymentView.getPaymentLines().get(0).getId()).isEqualTo(21);
    }

    @Test
    void testCreatePayment() {
        AuthUser authUser = new AuthUser().setRealm(REALM);
        EntityClass authEntity = new EntityClass().setId(AUTH_ENTITY_ID);
        PaymentView paymentView = new PaymentView().setAccountId(ACCOUNT_ID).setPaymentLines(List.of(
                new PaymentLineView().setAccountId(PREV_APPROVER).setAmount(new BigDecimal("1.0")),
                new PaymentLineView().setAccountId(PREV_APPROVER).setAmount(new BigDecimal("2.0"))
        ));
        when(permissionsService.getAuthEntity(authUser)).thenReturn(authEntity);
        when(permissionsService.checkWriteRights(authEntity, ACCOUNT_ID, REALM)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).then(invocation -> {
            Payment payment = invocation.getArgument(0);
            assertThat(payment.getCount()).isEqualTo(2);
            assertThat(payment.getTotal()).isEqualTo(new BigDecimal("3.0"));
            return payment.setId(4);
        });
        when(paymentLineRepository.save(any(PaymentLine.class))).then(invocation -> {
            PaymentLine paymentLine = invocation.getArgument(0);
            assertThat(paymentLine.getPaymentId()).isEqualTo(4);
            return paymentLine;
        });

        PaymentView actualPaymentView = service.createPayment(paymentView, authUser);
        assertThat(actualPaymentView.getId()).isEqualTo(4);
    }

    @Test
    void testApprovePayment() {
        AuthUser authUser = new AuthUser().setRealm(REALM);
        EntityClass authEntity = new EntityClass().setId(AUTH_ENTITY_ID);
        List<String> userRoles = List.of("ROLE1", "ROLE2");
        Account account = new Account().setId(ACCOUNT_ID).setAuthorisationType(AuthorisationType.SINGLE);
        Payment payment = new Payment().setId(PAYMENT_ID).setAccountId(ACCOUNT_ID).setStatus(PENDING).setMeta(new HashMap<>());
        List<PaymentLine> paymentLines = List.of(new PaymentLine());

        when(permissionsService.getAuthEntity(authUser)).thenReturn(authEntity);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(permissionsService.getRights(authEntity, account, REALM)).thenReturn(userRoles);
        when(paymentLineRepository.findByPaymentId(PAYMENT_ID)).thenReturn(paymentLines);

        PaymentView actualPayment = service.approvePayment(PAYMENT_ID, authUser);
        verify(permissionsService).checkApprovePermissions(account, userRoles, REALM, null);
        verify(paymentExecutionService).execute(payment, paymentLines);
        assertThat(actualPayment.getMeta().get(APPROVED_ID)).isEqualTo(AUTH_ENTITY_ID);
    }

    @Test
    void testApprovePaymentForDual() {
        AuthUser authUser = new AuthUser().setRealm(REALM);
        EntityClass authEntity = new EntityClass().setId(AUTH_ENTITY_ID);
        List<String> userRoles = List.of("ROLE1", "ROLE2");
        Account account = new Account().setId(ACCOUNT_ID).setAuthorisationType(AuthorisationType.DUAL_OR);
        Payment payment = new Payment().setId(PAYMENT_ID).setAccountId(ACCOUNT_ID).setStatus(PENDING).setMeta(new HashMap<>());

        when(permissionsService.getAuthEntity(authUser)).thenReturn(authEntity);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(permissionsService.getRights(authEntity, account, REALM)).thenReturn(userRoles);

        PaymentView actualPayment = service.approvePayment(PAYMENT_ID, authUser);
        verify(permissionsService).checkApprovePermissions(account, userRoles, REALM, null);
        verify(paymentRepository).save(payment);
        assertThat(actualPayment.getMeta().get(APPROVED_ID)).isEqualTo(AUTH_ENTITY_ID);
    }

    @Test
    void testSecondApprovePaymentForDual() {
        AuthUser authUser = new AuthUser().setRealm(REALM);
        EntityClass authEntity = new EntityClass().setId(AUTH_ENTITY_ID);
        List<String> userRoles = List.of("ROLE1", "ROLE2");
        Account account = new Account().setId(ACCOUNT_ID).setAuthorisationType(AuthorisationType.DUAL_OR);
        Payment payment = new Payment().setId(PAYMENT_ID).setAccountId(ACCOUNT_ID).setStatus(PENDING)
                .setMeta(new HashMap<>(Map.of(APPROVED_ID, PREV_APPROVER)));
        List<PaymentLine> paymentLines = List.of(new PaymentLine());

        when(permissionsService.getAuthEntity(authUser)).thenReturn(authEntity);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(permissionsService.getRights(authEntity, account, REALM)).thenReturn(userRoles);
        when(paymentLineRepository.findByPaymentId(PAYMENT_ID)).thenReturn(paymentLines);

        PaymentView actualPayment = service.approvePayment(PAYMENT_ID, authUser);
        verify(permissionsService).checkApprovePermissions(account, userRoles, REALM, PREV_APPROVER);
        verify(paymentExecutionService).execute(payment, paymentLines);
        assertThat(actualPayment.getMeta().get(APPROVED2_ID)).isEqualTo(AUTH_ENTITY_ID);
    }

    @Test
    void testApprovePaymentNotPending() {
        AuthUser authUser = new AuthUser().setRealm(REALM);
        when(permissionsService.getAuthEntity(authUser)).thenReturn(new EntityClass().setId(AUTH_ENTITY_ID));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(
                new Payment().setId(PAYMENT_ID).setAccountId(ACCOUNT_ID).setStatus(PROCESSING).setMeta(new HashMap<>())));

        ICEcashException exception = assertThrows(ICEcashException.class,
                () -> service.approvePayment(PAYMENT_ID, authUser));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1027);
    }

    @Test
    void testRejectPayment() {
        AuthUser authUser = new AuthUser().setRealm(REALM);
        Payment payment = new Payment().setId(PAYMENT_ID).setAccountId(ACCOUNT_ID);
        EntityClass authEntity = new EntityClass().setId(AUTH_ENTITY_ID);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(permissionsService.getAuthEntity(authUser)).thenReturn(authEntity);
        when(permissionsService.checkWriteRights(authEntity, ACCOUNT_ID, REALM)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).then(invocation -> invocation.getArgument(0));

        PaymentView resultPaymentView = service.rejectPayment(PAYMENT_ID, authUser);
        assertThat(resultPaymentView.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    }
}