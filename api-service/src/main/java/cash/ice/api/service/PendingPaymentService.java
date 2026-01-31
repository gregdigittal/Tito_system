package cash.ice.api.service;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentCollectionView;
import cash.ice.api.dto.PaymentView;
import cash.ice.api.errors.BulkPaymentParseException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.io.InputStream;

public interface PendingPaymentService {

    Page<PaymentView> getPayments(PageRequest pageRequest, AuthUser authUser);

    PaymentView getPayment(Integer paymentId, AuthUser authUser);

    PaymentView createPayment(PaymentView paymentView, AuthUser authUser);

    PaymentView updatePayment(PaymentView paymentView, AuthUser authUser);

    PaymentView uploadPaymentLines(Integer paymentId, String template, InputStream inputStream, AuthUser authUser) throws BulkPaymentParseException;

    PaymentView approvePayment(Integer paymentId, AuthUser authUser);

    PaymentView rejectPayment(Integer paymentId, AuthUser authUser);

    PaymentCollectionView createPaymentCollection(PaymentCollectionView paymentCollectionView, AuthUser authUser);

    void rejectPaymentCollection(Integer paymentCollectionId, AuthUser authUser);

    void approvePaymentCollection(Integer paymentCollectionId, AuthUser authUser);
}
