package cash.ice.ecocash.service.impl;

import cash.ice.common.constant.EntityMetaKey;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.common.service.KafkaSender;
import cash.ice.ecocash.config.EcocashProperties;
import cash.ice.ecocash.dto.EcocashCallbackPayment;
import cash.ice.ecocash.dto.EcocashCallbackPaymentResponse;
import cash.ice.ecocash.dto.Payment;
import cash.ice.ecocash.entity.EcocashPayment;
import cash.ice.ecocash.repository.EcocashMerchantRepository;
import cash.ice.ecocash.repository.EcocashPaymentRepository;
import cash.ice.ecocash.service.EcocashSenderService;
import error.EcocashException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;

import static cash.ice.common.error.ErrorCodes.EC6004;

@Service
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class EcocashPaymentServiceUatImpl extends EcocashPaymentServiceImpl {
    private static final String PENDING_SUBSCRIBER_VALIDATION = "PENDING SUBSCRIBER VALIDATION";

    public EcocashPaymentServiceUatImpl(EcocashProperties ecocashProperties, EcocashPaymentRepository ecocashPaymentRepository, EcocashMerchantRepository ecocashMerchantRepository, EcocashSenderService ecocashSenderService, KafkaSender kafkaSender) {
        super(ecocashProperties, ecocashPaymentRepository, ecocashMerchantRepository, ecocashSenderService, kafkaSender);
    }

    private static Map<String, Object> getMetaData(Object pendingPayment) {
        if (pendingPayment instanceof FeesData feesData) {
            return feesData.getPaymentRequest().getMeta();
        } else if (pendingPayment instanceof PaymentRequestZim paymentRequest) {
            return paymentRequest.getMetaData();
        } else {
            throw new UnsupportedOperationException("Unsupported payment type: " + pendingPayment.getClass());
        }
    }

    @Override
    public void processPayment(Payment payment, Headers headers) {
        super.processPayment(payment, headers);
        testActionIfNeed(payment);
    }

    private void testActionIfNeed(Payment payment) {
        EcocashPayment ecocashPayment = getEcocashPayment(payment.getVendorRef());
        if (payment.getMetaData().containsKey("performTestFail") || payment.getMetaData().containsKey("simulateEcocashFailAfterSuccess")) {
            log.info("Test payment fail for {}", payment.getVendorRef());
            throw new EcocashException(ecocashPayment, "Fail for test purpose", "202-IC1182-0000");
        } else if (payment.getMetaData().containsKey("performTestTimeout")) {
            log.info("Test payment expire for {}", payment.getVendorRef());
            handleResponse(ecocashPayment.getInitialResponse(), ecocashPayment, 1000);
        }
    }

    @Override
    protected void performAfterPaymentAction(EcocashPayment ecocashPayment) {
        if (getMetaData(ecocashPayment.getPendingPayment()).containsKey("performAfterPaymentFail")) {
            log.info("Test after payment fail for {}", ecocashPayment.getVendorRef());
            throw new EcocashException(ecocashPayment, "Fail after payment for test purpose", "202-IC1182-0000");
        } else {
            super.performAfterPaymentAction(ecocashPayment);
        }
    }

    @Override
    public int getStatusPollInitDelay(EcocashPayment ecocashPayment) {
        Integer statusPollInitDelay = (Integer) getMetaData(ecocashPayment.getPendingPayment()).get("statusPollInitDelay");
        return statusPollInitDelay != null ? statusPollInitDelay : super.getStatusPollInitDelay(ecocashPayment);
    }

    @Override
    protected int getStatusPollTimeout(EcocashPayment ecocashPayment) {
        Integer statusPollTimeout = (Integer) getMetaData(ecocashPayment.getPendingPayment()).get("statusPollTimeout");
        return statusPollTimeout != null ? statusPollTimeout : super.getStatusPollTimeout(ecocashPayment);
    }

    @Override
    protected EcocashCallbackPaymentResponse sendPayment(EcocashPayment ecocashPayment, EcocashCallbackPayment request) {
        Map<String, String> simulateResponseMap = ecocashProperties.getSimulateResponse();
        String simulate = (String) getMetaData(ecocashPayment.getPendingPayment()).get(EntityMetaKey.Simulate);
        if (simulateResponseMap.containsKey(request.getEndUserId()) || StringUtils.isNotBlank(simulate)) {
            String transactionOperationStatus = (String) getMetaData(ecocashPayment.getPendingPayment()).get("simulateEcocashFirstResponse");
            if (transactionOperationStatus == null) {
                transactionOperationStatus = simulateResponseMap.get(request.getEndUserId());
            }
            return EcocashCallbackPaymentResponse.createSimulatedResponse(request,
                    transactionOperationStatus != null ? transactionOperationStatus : COMPLETED);
        } else {
            return super.sendPayment(ecocashPayment, request);
        }
    }

    @Override
    public void callbackResponse(EcocashCallbackPaymentResponse response) {
        EcocashPayment ecocashPayment = ecocashPaymentRepository.findByClientCorrelator(response.getClientCorrelator())
                .orElseThrow(() -> new EcocashException("No pending payment for clientCorrelator: " +
                        response.getClientCorrelator(), EC6004));
        if (getMetaData(ecocashPayment.getPendingPayment()).containsKey("ignoreCallbackResponse")) {
            log.info("  ignoring callback with status: {}", response.getTransactionOperationStatus());
        } else {
            super.callbackResponse(response);
        }
    }

    @Override
    protected EcocashCallbackPaymentResponse sendPaymentStatus(EcocashPayment ecocashPayment, String endUserId, String clientCorrelator) {
        Map<String, String> simulateResponseMap = ecocashProperties.getSimulateResponse();
        String simulate = (String) getMetaData(ecocashPayment.getPendingPayment()).get(EntityMetaKey.Simulate);
        String checkStatus = (String) getMetaData(ecocashPayment.getPendingPayment()).get("simulateEcocashCheckStatus");
        if (getMetaData(ecocashPayment.getPendingPayment()).containsKey("performPollSuccess") || COMPLETED.equals(checkStatus)) {
            return EcocashCallbackPaymentResponse.createSimulatedResponse(ecocashPayment.getRequest(), COMPLETED);
        } else if (getMetaData(ecocashPayment.getPendingPayment()).containsKey("performPollFail") || FAILED.equals(checkStatus)) {
            return EcocashCallbackPaymentResponse.createSimulatedResponse(ecocashPayment.getRequest(), FAILED);
        } else if (simulateResponseMap.containsKey(ecocashPayment.getRequest().getEndUserId()) || StringUtils.isNotBlank(simulate)) {
            return EcocashCallbackPaymentResponse.createSimulatedResponse(ecocashPayment.getRequest(), PENDING_SUBSCRIBER_VALIDATION);
        } else {
            return super.sendPaymentStatus(ecocashPayment, endUserId, clientCorrelator);
        }
    }

    @Override
    protected EcocashCallbackPaymentResponse resendPaymentStatus(EcocashPayment ecocashPayment, String endUserId, String clientCorrelator) {
        String simulate = (String) getMetaData(ecocashPayment.getPendingPayment()).get(EntityMetaKey.Simulate);
        if (StringUtils.isBlank(simulate)) {
            return super.resendPaymentStatus(ecocashPayment, endUserId, clientCorrelator);
        } else {
            log.warn("  simulating resend payment status, return failed status");
            return EcocashCallbackPaymentResponse.createSimulatedResponse(ecocashPayment.getRequest(), FAILED);
        }
    }

    @Override
    public void processError(EcocashException e) {
        super.processError(e);
        if (getMetaData(e.getEcocashPayment().getPendingPayment()).containsKey("performDoubleRefund") &&
                COMPLETED.equals(e.getEcocashPayment().getFinalResponse().getTransactionOperationStatus())) {
            refund(e.getEcocashPayment().getVendorRef());
        }
    }

    @Override
    protected EcocashCallbackPaymentResponse sendRefund(EcocashPayment ecocashPayment, EcocashCallbackPayment refundRequest) {
        Map<String, String> simulateResponseMap = ecocashProperties.getSimulateResponse();
        String simulate = (String) getMetaData(ecocashPayment.getPendingPayment()).get(EntityMetaKey.Simulate);
        if (simulateResponseMap.containsKey(refundRequest.getEndUserId()) || StringUtils.isNotBlank(simulate)) {
            return EcocashCallbackPaymentResponse.createSimulatedResponse(refundRequest, COMPLETED);
        } else {
            return super.sendRefund(ecocashPayment, refundRequest);
        }
    }
}
