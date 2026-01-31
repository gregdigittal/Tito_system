package cash.ice.api.controller.zim;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentCollectionView;
import cash.ice.api.dto.PaymentView;
import cash.ice.api.dto.SortInput;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.PaymentDocumentsService;
import cash.ice.api.service.PendingPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PendingPaymentsController {
    private final PendingPaymentService pendingPaymentService;
    private final PaymentDocumentsService paymentDocumentsService;
    private final AuthUserService authUserService;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public PaymentView addPendingPayment(@Argument PaymentView payment) {
        log.debug("> POST " + payment);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.createPayment(payment, authUser);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public PaymentView updatePendingPayment(@Argument int id, @Argument PaymentView payment) {
        payment.setId(id);
        log.debug("> Update {}", payment);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.updatePayment(payment, authUser);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Page<PaymentView> allPendingPayments(@Argument int page, @Argument int size, @Argument SortInput sort) {
        log.debug("> GET payments, page={}, size={}", page, size);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.getPayments(PageRequest.of(page, size), authUser);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public PaymentView pendingPaymentById(@Argument int id) {
        log.debug("> GET payment id={}", id);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.getPayment(id, authUser);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public PaymentView approvePayment(@Argument int paymentId) {
        log.debug("> Approve payment id={}", paymentId);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.approvePayment(paymentId, authUser);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public PaymentView rejectPayment(@Argument int paymentId) {
        log.debug("> Reject payment id={}", paymentId);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.rejectPayment(paymentId, authUser);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public PaymentCollectionView createPaymentCollection(@Argument PaymentCollectionView paymentCollection) {
        log.debug("> POST " + paymentCollection);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.createPaymentCollection(paymentCollection, authUser);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Boolean rejectPaymentCollection(@Argument int collectionId) {
        log.debug("> Reject payment collection id={}", collectionId);
        AuthUser authUser = getAuthUser();
        pendingPaymentService.rejectPaymentCollection(collectionId, authUser);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Boolean approvePaymentCollection(@Argument int collectionId) {
        log.debug("> Approve payment collection id={}", collectionId);
        AuthUser authUser = getAuthUser();
        pendingPaymentService.approvePaymentCollection(collectionId, authUser);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Boolean deletePendingPaymentDocument(@Argument int paymentId, @Argument String documentId) throws Exception {
        log.debug("> Delete pending payment document: {}", documentId);
        AuthUser authUser = getAuthUser();
        paymentDocumentsService.deleteDocument(paymentId, documentId, authUser);
        return true;
    }

    protected AuthUser getAuthUser() {
        return authUserService.getAuthUser();
    }
}
