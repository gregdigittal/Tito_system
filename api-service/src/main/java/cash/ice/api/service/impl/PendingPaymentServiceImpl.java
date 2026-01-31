package cash.ice.api.service.impl;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentCollectionView;
import cash.ice.api.dto.PaymentLineView;
import cash.ice.api.dto.PaymentView;
import cash.ice.api.entity.zim.Payment;
import cash.ice.api.entity.zim.PaymentCollection;
import cash.ice.api.entity.zim.PaymentStatus;
import cash.ice.api.errors.BulkPaymentParseException;
import cash.ice.api.errors.ForbiddenException;
import cash.ice.api.errors.PaymentNotFoundException;
import cash.ice.api.parser.PaymentsBulkParser;
import cash.ice.api.parser.PaymentsBulkParserFactory;
import cash.ice.api.repository.zim.PaymentCollectionRepository;
import cash.ice.api.repository.zim.PaymentRepository;
import cash.ice.api.service.PaymentExecutionService;
import cash.ice.api.service.PendingPaymentService;
import cash.ice.api.service.PermissionsService;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AuthorisationType;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.PaymentLine;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.PaymentLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static cash.ice.common.error.ErrorCodes.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PendingPaymentServiceImpl implements PendingPaymentService {
    public static final String CREATED_ID = "CreatedId";
    public static final String MODIFIED_ID = "ModifiedId";
    public static final String APPROVED_ID = "ApprovedId";
    public static final String APPROVED2_ID = "Approved2Id";
    public static final String REJECTED_ID = "RejectedId";
    public static final String MODIFIED_DATE = "ModifiedDate";
    public static final String APPROVED_DATE = "ApprovedDate";
    public static final String APPROVED2_DATE = "Approved2Date";
    public static final String REJECTED_DATE = "RejectedDate";
    public static final String IMPORT_TEMPLATE = "importTemplate";

    private final PermissionsService permissionsService;
    private final PaymentExecutionService paymentExecutionService;
    private final PaymentRepository paymentRepository;
    private final PaymentLineRepository paymentLineRepository;
    private final PaymentCollectionRepository paymentCollectionRepository;
    private final PaymentsBulkParserFactory paymentsBulkParserFactory;
    private final AccountRepository accountRepository;

    @Override
    public Page<PaymentView> getPayments(PageRequest pageRequest, AuthUser authUser) {
        Page<Payment> page = paymentRepository.findAll(pageRequest);
        List<PaymentView> paymentViews = page
                .stream().filter(payment -> permissionsService.checkReadRights(authUser, payment.getAccountId()))
                .map(payment -> PaymentView.create(payment, paymentLineRepository.findByPaymentId(payment.getId())))
                .toList();
        return new PageImpl<>(paymentViews, page.getPageable(), page.getTotalPages());
    }

    @Override
    public PaymentView getPayment(Integer paymentId, AuthUser authUser) {
        Payment payment = getPayment(paymentId);
        if (!permissionsService.checkReadRights(authUser, payment.getAccountId())) {
            throw new ForbiddenException("to get payment");
        }
        return PaymentView.create(payment, paymentLineRepository.findByPaymentId(paymentId));
    }

    @Override
    @Transactional
    public PaymentView createPayment(PaymentView paymentView, AuthUser authUser) {
        EntityClass authEntity = permissionsService.getAuthEntity(authUser);
        log.debug("  authEntity: {}, {} {}, account: {}, realm: {}", authEntity.getId(), authEntity.getFirstName(), authEntity.getLastName(),
                paymentView.getAccountId(), authUser.getRealm());
        if (!permissionsService.checkWriteRights(authEntity, paymentView.getAccountId(), authUser.getRealm())) {
            throw new ForbiddenException("to create payment");
        }
        removeSensitiveFields(paymentView);
        List<PaymentLineView> lines = paymentView.getPaymentLines();
        Payment savedPayment = paymentRepository.save(paymentView.toPayment()
                .setId(null)
                .setStatus(PaymentStatus.PENDING)
                .setCreatedDate(Tool.currentDateTime())
                .addMetaField(CREATED_ID, authEntity.getId())
                .setCount(lines == null ? 0 : lines.size())
                .setTotal(lines == null ? BigDecimal.ZERO : lines.stream()
                        .map(PaymentLineView::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)));
        if (lines != null) {
            log.debug("  saving {} payment lines, paymentId: {}", lines.size(), savedPayment.getId());
            lines.stream().map(PaymentLineView::toPaymentLine)
                    .peek(paymentLine -> paymentLine.setPaymentId(savedPayment.getId()))
                    .forEach(paymentLineRepository::save);
        }
        return PaymentView.create(savedPayment, paymentLineRepository.findByPaymentId(savedPayment.getId()));
    }

    @Override
    @Transactional
    public PaymentView updatePayment(PaymentView paymentView, AuthUser authUser) {
        EntityClass authEntity = permissionsService.getAuthEntity(authUser);
        log.debug("  authEntity: {}, {} {}, account: {}, realm: {}", authEntity.getId(), authEntity.getFirstName(), authEntity.getLastName(),
                paymentView.getAccountId(), authUser.getRealm());
        if (!permissionsService.checkWriteRights(authEntity, paymentView.getAccountId(), authUser.getRealm())) {
            throw new ForbiddenException("to update payment");
        } else if (paymentView.getId() == null) {
            throw new PaymentNotFoundException(null);
        }
        removeSensitiveFields(paymentView);
        Payment payment = paymentView.toPayment();
        mergeSensitiveFields(payment, getPayment(paymentView.getId()));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ICEcashException("Cannot update not PENDING payment", EC1027);
        }
        List<PaymentLineView> linesView = paymentView.getPaymentLines();
        if (linesView != null) {
            log.debug("  saving {} payment lines, paymentId: {}", linesView.size(), payment.getId());
            linesView.stream().map(PaymentLineView::toPaymentLine)
                    .peek(paymentLine -> paymentLine.setPaymentId(payment.getId()))
                    .forEach(paymentLineRepository::save);
        }
        List<PaymentLine> allLines = paymentLineRepository.findByPaymentId(payment.getId());
        Payment savedPayment = paymentRepository.save(payment
                .addMetaField(MODIFIED_ID, authEntity.getId())
                .addMetaField(MODIFIED_DATE, Tool.getZimDateTimeString())
                .setCount(allLines.size())
                .setTotal(allLines.stream()
                        .map(PaymentLine::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)));
        return PaymentView.create(savedPayment, allLines);
    }

    private void removeSensitiveFields(PaymentView paymentView) {
        paymentView.setPaymentCollectionId(null).setDocuments(null);
        Tool.remove(paymentView.getMeta(), List.of(CREATED_ID, MODIFIED_ID, APPROVED_ID, APPROVED2_ID,
                MODIFIED_DATE, APPROVED_DATE, APPROVED2_DATE, IMPORT_TEMPLATE));
        if (paymentView.getPaymentLines() != null) {
            paymentView.getPaymentLines().forEach(line -> line.setTransactionId(null));
        }
    }

    private void mergeSensitiveFields(Payment newPayment, Payment oldPayment) {
        newPayment.setId(oldPayment.getId())
                .setStatus(oldPayment.getStatus())
                .setCreatedDate(oldPayment.getCreatedDate())
                .setPaymentCollectionId(oldPayment.getPaymentCollectionId())
                .setDocuments(oldPayment.getDocuments());
        Tool.merge(oldPayment.getMeta(), newPayment.getMeta(), List.of(CREATED_ID, MODIFIED_ID,
                APPROVED_ID, APPROVED2_ID, MODIFIED_DATE, APPROVED_DATE, APPROVED2_DATE, IMPORT_TEMPLATE));
    }

    @Override
    @Transactional
    public PaymentView uploadPaymentLines(Integer paymentId, String template, InputStream inputStream, AuthUser authUser) throws BulkPaymentParseException {
        Payment payment = getPayment(paymentId);
        if (!permissionsService.checkWriteRights(authUser, payment.getAccountId())) {
            throw new ForbiddenException("to upload payment lines");
        } else if (payment.getMeta() != null && payment.getMeta().containsKey(IMPORT_TEMPLATE)) {
            throw new ICEcashException("Template already loaded: " + payment.getMeta().get(IMPORT_TEMPLATE), EC1029);
        }
        PaymentsBulkParser parser = paymentsBulkParserFactory.getParser(template);
        log.debug("  template: {}, parser: {}", template, parser.getClass().getName());
        List<PaymentLine> lines = parser.parseExcelStream(inputStream, payment);
        log.debug("  parsed {} lines, paymentId: {}", lines.size(), payment.getId());
        lines.stream().peek(paymentLine -> paymentLine.setPaymentId(payment.getId()))
                .forEach(paymentLineRepository::save);

        List<PaymentLine> allLines = paymentLineRepository.findByPaymentId(payment.getId());
        payment.addMetaField(IMPORT_TEMPLATE, template)
                .setCount(allLines.size())
                .setTotal(allLines.stream()
                        .map(PaymentLine::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        paymentRepository.save(payment);
        return PaymentView.create(payment, allLines);
    }

    @Override
    @Transactional
    public PaymentView approvePayment(Integer paymentId, AuthUser authUser) {
        return approvePayment(getPayment(paymentId), authUser);
    }

    private PaymentView approvePayment(Payment payment, AuthUser authUser) {
        EntityClass authEntity = permissionsService.getAuthEntity(authUser);
        log.debug("  authEntity: {}, {} {}, account: {}, realm: {}", authEntity.getId(), authEntity.getFirstName(), authEntity.getLastName(),
                payment.getAccountId(), authUser.getRealm());
        List<PaymentLine> paymentLines = paymentLineRepository.findByPaymentId(payment.getId());
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ICEcashException("Cannot approve not PENDING payment", EC1027);
        }
        Account account = accountRepository.findById(payment.getAccountId()).orElseThrow(() ->
                new ICEcashException(String.format("Account with id=%s is absent", payment.getAccountId()), EC1022));
        List<String> rights = permissionsService.getRights(authEntity, account, authUser.getRealm());
        Integer prevApprover = (Integer) payment.getMeta().get(APPROVED_ID);
        log.debug("  rights: {}, entity: {} for account: {} realm: {}, prevApprover: {}", rights, authEntity.getId(),
                account.getId(), authUser.getRealm(), prevApprover);
        permissionsService.checkApprovePermissions(account, rights, authUser.getRealm(), prevApprover);
        payment.getMeta().put(prevApprover == null ? APPROVED_ID : APPROVED2_ID, authEntity.getId());
        payment.getMeta().put(prevApprover == null ? APPROVED_DATE : APPROVED2_DATE, Tool.getZimDateTimeString());
        if (account.getAuthorisationType() == null || account.getAuthorisationType() == AuthorisationType.SINGLE
                || prevApprover != null) {
            paymentRepository.save(payment.setStatus(PaymentStatus.PROCESSING));
            paymentExecutionService.execute(payment, paymentLines);
        } else {
            paymentRepository.save(payment);
        }
        return PaymentView.create(payment, paymentLines);
    }

    @Override
    public PaymentView rejectPayment(Integer paymentId, AuthUser authUser) {
        return rejectPayment(getPayment(paymentId), authUser);
    }

    private PaymentView rejectPayment(Payment payment, AuthUser authUser) {
        EntityClass authEntity = permissionsService.getAuthEntity(authUser);
        log.debug("  authEntity: {}, {} {}, account: {}, realm: {}", authEntity.getId(), authEntity.getFirstName(), authEntity.getLastName(),
                payment.getAccountId(), authUser.getRealm());
        if (!permissionsService.checkWriteRights(authEntity, payment.getAccountId(), authUser.getRealm())) {
            throw new ForbiddenException("to reject payment");
        }
        Payment rejectedPayment = paymentRepository.save(payment
                .addMetaField(REJECTED_ID, authEntity.getId())
                .addMetaField(REJECTED_DATE, Tool.getZimDateTimeString())
                .setStatus(PaymentStatus.REJECTED));
        return PaymentView.create(rejectedPayment, paymentLineRepository.findByPaymentId(rejectedPayment.getId()));
    }

    @Override
    @Transactional
    public PaymentCollectionView createPaymentCollection(PaymentCollectionView collectionView, AuthUser authUser) {
        PaymentCollection paymentCollection = paymentCollectionRepository.save(
                collectionView.toPaymentCollection().setCreatedDate(Tool.currentDateTime()));
        log.debug("  collection for payments: {}", collectionView.getPaymentsIds());
        List<Payment> payments = collectionView.getPaymentsIds().stream().map(paymentId -> {
            Payment payment = getPayment(paymentId);
            if (!permissionsService.checkWriteRights(authUser, payment.getAccountId())) {
                throw new ForbiddenException("to assign payment to a collection");
            }
            return paymentRepository.save(payment.setPaymentCollectionId(paymentCollection.getId()));
        }).toList();
        return PaymentCollectionView.create(paymentCollection)
                .setPayments(payments.stream().map(payment ->
                        PaymentView.create(payment, null)).toList());
    }

    @Override
    @Transactional
    public void rejectPaymentCollection(Integer paymentCollectionId, AuthUser authUser) {
        if (!paymentCollectionRepository.existsById(paymentCollectionId)) {
            throw new ICEcashException("Unknown payment collection ID", EC1024);
        }
        List<Payment> payments = paymentRepository.findByPaymentCollectionId(paymentCollectionId);
        if (payments.isEmpty()) {
            throw new ICEcashException("No payments assigned to a collection", EC1025);
        }
        payments.forEach(payment -> rejectPayment(payment, authUser));
    }

    @Override
    @Transactional
    public void approvePaymentCollection(Integer paymentCollectionId, AuthUser authUser) {
        if (!paymentCollectionRepository.existsById(paymentCollectionId)) {
            throw new ICEcashException("Unknown payment collection ID", EC1024);
        }
        List<Payment> payments = paymentRepository.findByPaymentCollectionId(paymentCollectionId);
        if (payments.isEmpty()) {
            throw new ICEcashException("No payments assigned to a collection", EC1025);
        }
        payments.forEach(payment -> approvePayment(payment, authUser));
    }

    private Payment getPayment(Integer paymentId) {
        return paymentRepository.findById(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
