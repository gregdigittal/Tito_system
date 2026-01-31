package cash.ice.emola.service;

import cash.ice.common.dto.BeneficiaryNameResponse;
import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.PaymentService;
import cash.ice.emola.entity.EmolaPayment;
import org.apache.kafka.common.header.Headers;

public interface EmolaPaymentService extends PaymentService {

    void processPayment(FeesData feesData, Headers headers);

    void failPayment(FeesData feesData, String errorCode, String reason, Headers headers);

    void failPayment(EmolaPayment emolaPayment, FeesData feesData, String errorCode, String reason, Headers headers);

    void processRefund(ErrorData errorData);

    BeneficiaryNameResponse queryCustomerName(String msisdn);
}
