package cash.ice.paygo.service.impl;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.paygo.dto.DirectoryService;
import cash.ice.paygo.dto.PaygoCallbackRequest;
import cash.ice.paygo.dto.PaygoCallbackResponse;
import cash.ice.paygo.dto.RequestedPayment;
import cash.ice.paygo.entity.PaygoPayment;
import cash.ice.paygo.repository.PaygoPaymentRepository;
import cash.ice.paygo.service.PaygoCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static cash.ice.common.error.ErrorCodes.*;
import static cash.ice.paygo.listener.PaygoServiceListener.SERVICE_HEADER;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaygoCallbackServiceImpl implements PaygoCallbackService {
    private final PaygoPaymentRepository paygoPaymentRepository;
    private final KafkaSender kafkaSender;

    @Override
    public PaygoCallbackResponse handleRequest(PaygoCallbackRequest request) {
        FeesData pendingPayment = null;
        String deviceReference = null;
        try {
            PaygoPayment paygoPayment = paygoPaymentRepository.findByPayGoId(request.getPayee()).orElseThrow(
                    () -> new ICEcashException("No pending payment, unknown PayGoId: " + request.getPayee(), EC5008));
            pendingPayment = paygoPayment.getPendingPayment();
            deviceReference = paygoPayment.getDeviceReference();
            PaygoCallbackResponse response = PaygoCallbackResponse.create(request)
                    .setResponseCode("000")
                    .setVendorRef(pendingPayment.getPaymentRequest().getVendorRef());
            switch (request.getMessageType()) {
                case "AUTH" -> handleAuthRequest(request, response, paygoPayment);
                case "ADVICE" -> handleAdviceRequest(request, paygoPayment);
                default -> throw new ICEcashException("Unknown message type: " + request.getMessageType(), EC5004);
            }
            return response;

        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            String errorCode = e instanceof ICEcashException ? ((ICEcashException) e).getErrorCode() : EC5004;
            if (pendingPayment != null) {
                kafkaSender.sendErrorPayment(pendingPayment.getVendorRef(), new ErrorData(pendingPayment, errorCode,
                        String.format("%s (DeviceReference: %s, PayGoId: %s)", e.getMessage(), deviceReference, request.getPayee())));
            }
            return PaygoCallbackResponse.create(request).setResponseCode(getResponseErrorCode(errorCode));
        }
    }

    private void handleAuthRequest(PaygoCallbackRequest request, PaygoCallbackResponse response, PaygoPayment paygoPayment) {
        PaymentRequest paymentRequest = paygoPayment.getPendingPayment().getPaymentRequest();
        checkMerchant(paygoPayment);
        checkCredential(paygoPayment, request.getCurrencyCode());
        response.getAdditionalData().setDirectoryService(new DirectoryService()
                .setMerchantId(paygoPayment.getMerchant().getId())
                .setAuthorizedCredentialId(paygoPayment.getCredential().getId())
                .setRequestedPayment(new RequestedPayment()
                        .setAmount(paymentRequest.getAmount())
                        .setCurrencyCode(paymentRequest.getCurrency())
                        .setNarration((String) paymentRequest.getMeta().get("description"))
                        .setExpirySeconds(paygoPayment.getExpirySeconds())
                        .setInitiator(paygoPayment.getMerchant().getName()))
        );
        response.setDeviceReference(paygoPayment.getDeviceReference());
        response.setResponseDescription("APPROVED");
    }

    private void handleAdviceRequest(PaygoCallbackRequest request, PaygoPayment paygoPayment) {
        FeesData pendingPayment = paygoPayment.getPendingPayment();
        PaymentRequest paymentRequest = pendingPayment.getPaymentRequest();
        paygoPaymentRepository.delete(paygoPayment);
        if ("000".equals(request.getResponseCode())) {
            kafkaSender.sendPaygoSuccessPayment(paymentRequest.getVendorRef(), paygoPayment.getPendingPayment(), Tool.toKafkaHeaders(paygoPayment.getKafkaHeaders()), SERVICE_HEADER);
        } else {
            kafkaSender.sendErrorPayment(paymentRequest.getVendorRef(), new ErrorData(pendingPayment, EC5012,
                    String.format("PayGo DS returned: %s %s (DeviceReference: %s, PayGoId: %s)", request.getResponseCode(),
                            request.getResponseDescription(), paygoPayment.getDeviceReference(), request.getPayee())));
        }
    }

    private String getResponseErrorCode(String iceErrorCode) {
        if (EC5004.equals(iceErrorCode)) {
            return "006";
        } else {
            return EC5008.equals(iceErrorCode) || EC5010.equals(iceErrorCode) ? "003" : "001";
        }
    }

    private void checkMerchant(PaygoPayment paygoPayment) {
        if (paygoPayment.getMerchant() == null || paygoPayment.getMerchant().getId() == null) {
            throw new ICEcashException("Wrong Merchant, PaygoPayment: " + paygoPayment.getId(), EC5010);
        }
    }

    private void checkCredential(PaygoPayment paygoPayment, String currencyCode) {
        if (paygoPayment.getCredential() == null || paygoPayment.getCredential().getId() == null) {
            throw new ICEcashException("Wrong Credential, PaygoPayment: " + paygoPayment.getId(), EC5011);
        } else if (!Objects.equals(paygoPayment.getCredential().getCurrencyCode(), currencyCode)) {
            throw new ICEcashException(String.format("Requested wrong currencyCode: %s for PaygoPayment: %s with credential currency: %s",
                    currencyCode, paygoPayment.getId(), paygoPayment.getCredential().getCurrencyCode()), EC5011);
        }
    }
}
