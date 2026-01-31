package cash.ice.mpesa.service;

import cash.ice.common.dto.BeneficiaryNameResponse;
import cash.ice.mpesa.dto.Payment;
import cash.ice.mpesa.dto.ReversalStatus;
import cash.ice.mpesa.dto.TransactionStatus;
import cash.ice.mpesa.entity.MpesaPayment;
import cash.ice.mpesa.error.MpesaException;

public interface MpesaPaymentService {

    MpesaPayment processPayment(Payment payment);

    void processError(MpesaException e);

    void handleResponse(MpesaPayment mpesaPayment, Payment payment);

    MpesaPayment processRefund(String vendorRef);

    ReversalStatus manualRefund(String vendorRefOrTransactionId);

    BeneficiaryNameResponse queryCustomerName(String msisdn);

    TransactionStatus queryTransactionStatus(String transactionOrConversationOrVendorId);

    MpesaPayment getMpesaPayment(String vendorRef);

    MpesaPayment getMpesaPaymentByTransactionId(String transactionId);
}
