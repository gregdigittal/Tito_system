package cash.ice.ecocash.service;

import cash.ice.ecocash.dto.EcocashCallbackPayment;
import cash.ice.ecocash.dto.EcocashCallbackPaymentResponse;

public interface EcocashSenderService {

    EcocashCallbackPaymentResponse sendPayment(EcocashCallbackPayment request);

    EcocashCallbackPaymentResponse requestPaymentStatus(String msisdn, String clientCorrelator);

    EcocashCallbackPaymentResponse refundPayment(EcocashCallbackPayment refundRequest);
}
