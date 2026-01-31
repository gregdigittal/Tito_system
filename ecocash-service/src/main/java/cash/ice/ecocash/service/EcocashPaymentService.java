package cash.ice.ecocash.service;

import cash.ice.ecocash.dto.EcocashCallbackPaymentResponse;
import cash.ice.ecocash.dto.Payment;
import cash.ice.ecocash.dto.ReversalStatus;
import cash.ice.ecocash.entity.EcocashPayment;
import error.EcocashException;
import org.apache.kafka.common.header.Headers;

public interface EcocashPaymentService {

    void processPayment(Payment payment, Headers headers);

    void callbackResponse(EcocashCallbackPaymentResponse response);

    void checkStatus(EcocashPayment ecocashPayment, long paymentDurationSeconds);

    void recheckStatus(EcocashPayment ecocashPayment);

    int getStatusPollInitDelay(EcocashPayment ecocashPayment);

    void processError(EcocashException e);

    EcocashPayment refund(String vendorRef);

    ReversalStatus manualRefund(String vendorRef);

    EcocashPayment getEcocashPayment(String vendorRef);
}
