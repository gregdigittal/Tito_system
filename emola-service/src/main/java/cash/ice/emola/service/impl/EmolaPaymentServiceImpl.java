package cash.ice.emola.service.impl;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.BeneficiaryNameResponse;
import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.emola.config.EmolaProperties;
import cash.ice.emola.dto.EmolaRequest;
import cash.ice.emola.dto.EmolaResponse;
import cash.ice.emola.entity.EmolaPayment;
import cash.ice.emola.error.EmolaException;
import cash.ice.emola.listener.EmolaServiceListener;
import cash.ice.emola.repository.EmolaPaymentRepository;
import cash.ice.emola.service.EmolaPaymentService;
import cash.ice.emola.service.EmolaSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Profile(IceCashProfile.PROD)
@Slf4j
public class EmolaPaymentServiceImpl implements EmolaPaymentService {
    protected final EmolaSenderService emolaSenderService;
    protected final KafkaSender kafkaSender;
    protected final EmolaPaymentRepository emolaPaymentRepository;
    protected final EmolaProperties emolaProperties;

    @Override
    public void processPayment(FeesData feesData, Headers headers) {
        log.info(">>> Emola process payment for vendorRef: {}", feesData.getVendorRef());

        EmolaPayment emolaPayment = emolaPaymentRepository.save(new EmolaPayment()
                .setVendorRef(feesData.getVendorRef())
                .setTransactionCode(feesData.getTransactionCode())
                .setTransactionId(Tool.generateCharacters(20))
                .setMsisdn(feesData.getPaymentRequest().getInitiator())
                .setAmount(feesData.getPaymentRequest().getAmount())
                .setCreatedTime(Instant.now()));
        EmolaResponse emolaResponse = sendPayment(feesData, emolaPayment);
        if (emolaResponse != null) {
            log.debug("  received: {}, code: {}, orig: {}, response: {}", emolaResponse.getOriginalMessage(),
                    emolaResponse.getErrorCode(), emolaResponse.getOriginalResponseCode(), emolaResponse);
            emolaPayment.setResponseErrorCode(emolaResponse.getErrorCode())
                    .setResponseDescription(emolaResponse.getDescription())
                    .setResponseGwtransid(emolaResponse.getGwtransid())
                    .setResponseOriginalErrorCode(emolaResponse.getOriginalErrorCode())
                    .setResponseOriginalMessage(emolaResponse.getOriginalMessage())
                    .setResponseOriginalRequestId(emolaResponse.getOriginalRequestId());
        } else {
            log.debug("  no response");
            emolaPayment.setResponseDescription("no response");
        }
        handleResponse(emolaPayment, feesData, headers);
    }

    private void handleResponse(EmolaPayment emolaPayment, FeesData feesData, Headers headers) {
        if (emolaPayment.isFinishedPayment()) {
            log.debug("  Successful payment for " + emolaPayment.getVendorRef());
            emolaPaymentRepository.save(emolaPayment.setUpdatedTime(Instant.now()));
            handleSuccessfulPayment(emolaPayment.getVendorRef(), feesData, headers);
        } else {
            throw new EmolaException(emolaPayment, String.format("Received %s error: %s %s", emolaPayment.getResponseErrorCode(),
                    emolaPayment.getResponseOriginalErrorCode(), emolaPayment.getResponseOriginalMessage()), ErrorCodes.EC9102);
        }
    }

    protected void handleSuccessfulPayment(String vendorRef, FeesData feesData, Headers headers) {
        kafkaSender.sendEmolaSuccessPayment(vendorRef, feesData, headers, EmolaServiceListener.SERVICE_HEADER);
    }

    protected EmolaResponse sendPayment(FeesData feesData, EmolaPayment emolaPayment) {
        return switch (feesData.getTransactionCode()) {
            case "EMI" -> sendRequest(feesData, emolaPayment, new EmolaRequest()
                    .setWscode("pushUssdMessage").setParams(Map.of(
                            "partnerCode", emolaProperties.getPartnerCode(),
                            "msisdn", feesData.getPaymentRequest().getInitiator(),
                            "smsContent", emolaProperties.getInboundSmsContent(),
                            "transAmount", feesData.getPaymentRequest().getAmount().toString(),
                            "transId", emolaPayment.getTransactionId(),
                            "language", "en",                                   // todo
                            "refNo", feesData.getVendorRef(),
                            "key", emolaProperties.getPrivateKey()
                    )));
            case "EMO" -> sendRequest(feesData, emolaPayment, new EmolaRequest()
                    .setWscode("pushUssdDisbursementB2C").setParams(Map.of(
                            "transId", emolaPayment.getTransactionId(),
                            "partnerCode", emolaProperties.getPartnerCode(),
                            "msisdn", feesData.getPaymentRequest().getInitiator(),
                            "transAmount", feesData.getPaymentRequest().getAmount().toString(),
                            "smsContent", emolaProperties.getInboundSmsContent(),
                            "key", emolaProperties.getPrivateKey()
                    )));
            default -> throw new EmolaException(emolaPayment, "Wrong transaction code", ErrorCodes.EC9103);
        };
    }

    protected EmolaResponse sendRequest(FeesData feesData, EmolaPayment emolaPayment, EmolaRequest request) {
        try {
            return emolaSenderService.sendRequest(request);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new EmolaException(emolaPayment, e.getMessage(), ErrorCodes.EC9102);
        }
    }

    protected EmolaPayment getEmolaPayment(String vendorRef) {
        List<EmolaPayment> payments = emolaPaymentRepository.findByVendorRef(vendorRef);
        return payments.isEmpty() ? null : payments.get(payments.size() - 1);
    }


    @Override
    public void failPayment(FeesData feesData, String errorCode, String reason, Headers headers) {
        EmolaPayment emolaPayment = getEmolaPayment(feesData.getVendorRef());
        if (emolaPayment != null) {
            failPayment(emolaPayment, feesData, errorCode, reason, headers);
        } else {
            log.debug("  Failed payment, reason: {}, errorCode: {}, for {}", reason, errorCode, feesData.getVendorRef());
            kafkaSender.sendErrorPayment(feesData.getVendorRef(), new ErrorData(feesData, errorCode, reason), headers);
        }
    }

    @Override
    public void failPayment(EmolaPayment emolaPayment, FeesData feesData, String errorCode, String reason, Headers headers) {
        emolaPaymentRepository.save(emolaPayment
                .setErrorCode(errorCode)
                .setErrorMessage(reason)
                .setUpdatedTime(Instant.now()));
        log.debug("  Failed payment, reason: {}, errorCode: {}, for {}", reason, errorCode, emolaPayment.getVendorRef());
        kafkaSender.sendErrorPayment(emolaPayment.getVendorRef(), new ErrorData(feesData, errorCode, reason), headers);
        if (emolaPayment.isFinishedPayment()) {
            log.info("  COMPLETED response for FAILED payment, need refund");
            refund(emolaPayment.getVendorRef(), feesData);
        }
    }

    @Override
    public void processRefund(ErrorData errorData) {
        String vendorRef = errorData.getFeesData().getVendorRef();
        log.info(">>>>>> Emola process refund for vendorRef: {}", vendorRef);
        refund(vendorRef, errorData.getFeesData());
    }

    protected void refund(String vendorRef, FeesData feesData) {
        EmolaPayment emolaPayment = getEmolaPayment(vendorRef);
        if (emolaPayment == null) {
            log.error("No pending payment to refund for vendorRef: {}", vendorRef);
        } else if (emolaPayment.isFinishedPayment() && !emolaPayment.isRefunded()) {
            // todo add refund logic
        } else {
            log.warn("  refund, but payment successful: {}, already refunded: {}, for {}", emolaPayment.isFinishedPayment(), emolaPayment.isRefunded(), emolaPayment.getVendorRef());
        }
    }

    @Override
    public BeneficiaryNameResponse queryCustomerName(String msisdn) {
        BeneficiaryNameResponse nameResponse = new BeneficiaryNameResponse().setMsisdn(msisdn);
        EmolaResponse emolaResponse = emolaSenderService.sendRequest(new EmolaRequest()
                .setWscode("queryBeneficiaryName").setParams(Map.of(
                        "transId", Tool.generateCharacters(20),
                        "partnerCode", emolaProperties.getPartnerCode(),
                        "msisdn", msisdn,
                        "key", emolaProperties.getPrivateKey()
                )));
        nameResponse.setName(emolaResponse.getOriginalMessage());
        if (!"0".equals(emolaResponse.getErrorCode())) {
            nameResponse.setErrorCode(ErrorCodes.EC9102);
            nameResponse.setErrorMessage(String.format("Received %s error: %s", emolaResponse.getOriginalErrorCode(), emolaResponse.getOriginalMessage()));
        }
        log.debug("  response: {}, orig: {}, {}", emolaResponse.getErrorCode(), emolaResponse.getOriginalErrorCode(), emolaResponse.getOriginalMessage());
        return nameResponse;
    }
}
