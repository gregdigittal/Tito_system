package cash.ice.api.service;

import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;

import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

public interface PaymentService {

    void addPayment(PaymentRequest paymentRequest);

    PaymentResponse makePaymentSynchronous(PaymentRequest paymentRequest);

    PaymentResponse makePaymentSynchronous(PaymentRequest paymentRequest, Duration maxWaitDuration, BiConsumer<PaymentRequest, PaymentResponse> afterPaymentAction);

    List<PaymentResponse> makeBulkPaymentSynchronous(List<PaymentRequest> paymentRequestList, boolean ticketForFail, Duration maxWaitDuration, BiConsumer<PaymentRequest, PaymentResponse> afterPaymentAction);

    PaymentRequest getPaymentRequest(String vendorRef);

    PaymentResponse getPaymentResponse(String vendorRef);
}
