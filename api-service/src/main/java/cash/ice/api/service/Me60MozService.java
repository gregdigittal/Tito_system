package cash.ice.api.service;

import cash.ice.api.dto.moz.LinkNfcTagRequest;
import cash.ice.api.dto.moz.MozAccountInfoResponse;
import cash.ice.api.dto.moz.MozAutoRegisterDeviceRequest;
import cash.ice.api.dto.moz.TagLinkResponse;
import cash.ice.api.dto.moz.OffloadPaymentRequestMoz;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.api.dto.moz.PaymentResponseMoz;
import cash.ice.sqldb.entity.Initiator;
import cash.ice.sqldb.entity.moz.Device;

import java.util.List;
import java.util.Map;

public interface Me60MozService {

    String registerDevice(MozAutoRegisterDeviceRequest request);

    String linkTag(LinkNfcTagRequest request);

    TagLinkResponse linkTagValidateOtp(LinkNfcTagRequest request);

    Initiator linkTagRegister(LinkNfcTagRequest request);

    void cleanupExpiredMozLinkTagTask();

    MozAccountInfoResponse getAccountInfo(String deviceSerial);

    PaymentResponse makePayment(PaymentRequest paymentRequest);

    List<PaymentResponse> makeBulkPayment(List<PaymentRequest> paymentRequestList);

    PaymentResponse makeOffloadPayment(OffloadPaymentRequestMoz offloadPaymentRequest);

    PaymentResponse makeBulkPayments(List<PaymentRequest> payments);

    Map<String, PaymentResponseMoz.BalanceResponse> getAccountsBalances(Integer entityId, List<Integer> balanceAccountTypes);

    String getOtp(String requestId);

    Initiator addClearTag(String tag);

    Initiator removeTag(String tag);

    Device activateDevice(String serialOrCode, String accountNumber);

    Device removeDevice(String serialOrCode);
}
