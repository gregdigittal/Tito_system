package cash.ice.fbc.service.impl;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.service.PaymentService;
import cash.ice.common.utils.Tool;
import cash.ice.fbc.config.FlexcubeProperties;
import cash.ice.fbc.dto.flexcube.FlexcubeBalanceResponse;
import cash.ice.fbc.dto.flexcube.FlexcubePaymentRequest;
import cash.ice.fbc.dto.flexcube.FlexcubeResponse;
import cash.ice.fbc.dto.flexcube.FlexcubeStatusRequest;
import cash.ice.fbc.entity.FlexcubeAccount;
import cash.ice.fbc.entity.FlexcubePayment;
import cash.ice.fbc.error.FlexcubeException;
import cash.ice.fbc.listener.RtgsServiceListener;
import cash.ice.fbc.repository.FlexcubeAccountRepository;
import cash.ice.fbc.repository.FlexcubePaymentRepository;
import cash.ice.fbc.service.FlexcubeBalanceService;
import cash.ice.fbc.service.FlexcubeSenderService;
import cash.ice.fbc.service.FlexcubeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static cash.ice.common.error.ErrorCodes.EC8002;
import static cash.ice.common.error.ErrorCodes.EC8003;

@Service
@Profile(IceCashProfile.PROD)
@RequiredArgsConstructor
@Slf4j
public class FlexcubeServiceImpl implements PaymentService, FlexcubeService {
    private final FlexcubeProperties flexcubeProperties;
    private final FlexcubeBalanceService flexcubeBalanceService;
    private final FlexcubeSenderService flexcubeSenderService;
    private final FlexcubeAccountRepository flexcubeAccountRepository;
    private final FlexcubePaymentRepository flexcubePaymentRepository;
    private final KafkaSender kafkaSender;

    @Override
    public void processPayment(FeesData feesData, Headers headers) {
        log.info(">>> Flexcube process payment for vendorRef: {}", feesData.getVendorRef());
        FlexcubePayment flexcubePayment = new FlexcubePayment()
                .setCreatedTime(Instant.now())
                .setVendorRef(feesData.getVendorRef())
                .setPendingPayment(feesData)
                .setKafkaHeaders(Tool.toKafkaHeaderKeys(headers));
        FlexcubeAccount fbcPoolAccount = getFbcPoolAccount(flexcubePayment, feesData.getTransactionCode());
        FlexcubeBalanceResponse balanceResponse = checkBalance(fbcPoolAccount,
                feesData.getPaymentRequest().getAmount(), flexcubePayment);

        FlexcubePaymentRequest request = FlexcubePaymentRequest.create(feesData, fbcPoolAccount, flexcubeProperties);
        flexcubePaymentRepository.save(flexcubePayment
                .setRequest(request)
                .setReferenceId(request.getExternalReference()));

        FlexcubeResponse response = sendPayment(request, flexcubePayment);
        log.debug("  response: {} {}, for vendorRef: {}", response.getResultCode(), response.getResultDescription(), feesData.getVendorRef());
        flexcubePayment.setResponse(response)
                .setHostReference(response.getHostReference())
                .setResponseResult(response.getResultCode() + " " + response.getResultDescription())
                .setFinishedPayment(true)
                .setUpdatedTime(Instant.now());

        if ("00".equals(response.getResultCode())) {
            handleSuccessResponse(flexcubePayment);
            updateBalanceCache(balanceResponse, fbcPoolAccount, flexcubePayment);
        } else {
            throw new FlexcubeException(flexcubePayment, "Received: " + flexcubePayment.getResponseResult(), EC8002);
        }
    }

    @Override
    public void checkStatus(FlexcubePayment payment) {
        FlexcubeStatusRequest request = new FlexcubeStatusRequest(String.valueOf(payment.getReferenceId()));
        FlexcubeResponse response = sendCheck(request, payment);
        log.debug("  status response: {} {}, for vendorRef: {}", response.getResultCode(), response.getResultDescription(), payment.getVendorRef());
        payment.setStatusRequest(request)
                .setResponse(response)
                .setHostReference(response.getHostReference())
                .setResponseResult(response.getResultCode() + " " + response.getResultDescription())
                .setFinishedPayment(true)
                .setUpdatedTime(Instant.now());
        log.debug("  response: {} {}, for vendorRef: {}", response.getResultCode(), response.getResultDescription(), payment.getVendorRef());

        if ("00".equals(response.getResultCode())) {
            handleSuccessResponse(payment);
            FlexcubeAccount fbcPoolAccount = getFbcPoolAccount(payment, payment.getPendingPayment().getTransactionCode());
            flexcubeSenderService.evictBalance(fbcPoolAccount.getDebitPoolAccount(), fbcPoolAccount.getDebitPoolAccountBranch());
        } else {
            failPayment(payment, EC8002, "Received: " + payment.getResponseResult(),
                    Tool.toKafkaHeaders(payment.getKafkaHeaders()));
        }
    }

    private void handleSuccessResponse(FlexcubePayment payment) {
        log.debug("  Success response for " + payment.getVendorRef());
        kafkaSender.sendRtgsSuccessPayment(payment.getVendorRef(), payment.getPendingPayment(),
                Tool.toKafkaHeaders(payment.getKafkaHeaders()), RtgsServiceListener.SERVICE_HEADER);
        flexcubePaymentRepository.save(payment);
    }

    protected FlexcubeBalanceResponse checkBalance(FlexcubeAccount fbcPoolAccount, BigDecimal amount, FlexcubePayment payment) {
        return flexcubeBalanceService.checkBalance(fbcPoolAccount, amount, payment);
    }

    protected FlexcubeResponse sendPayment(FlexcubePaymentRequest request, FlexcubePayment payment) {
        return flexcubeSenderService.sendPayment(request);
    }

    protected FlexcubeResponse sendCheck(FlexcubeStatusRequest request, FlexcubePayment payment) {
        return flexcubeSenderService.sendCheck(request);
    }

    protected void updateBalanceCache(FlexcubeBalanceResponse balanceResponse, FlexcubeAccount fbcPoolAccount, FlexcubePayment payment) {
        flexcubeBalanceService.updateBalanceCache(balanceResponse, fbcPoolAccount, payment);
    }

    private FlexcubeAccount getFbcPoolAccount(FlexcubePayment flexcubePayment, String transactionCode) {
        List<FlexcubeAccount> accounts = flexcubeAccountRepository.findByTransactionCodesIn(transactionCode);
        if (!accounts.isEmpty()) {
            return accounts.get(0);
        } else {
            throw new FlexcubeException(flexcubePayment, "No FBC Pool account configured", EC8003);
        }
    }

    @Override
    public void failPayment(FeesData feesData, String errorCode, String reason, Headers headers) {
        FlexcubePayment flexcubePayment = getFlexcubePayment(feesData.getVendorRef());
        if (flexcubePayment != null) {
            failPayment(flexcubePayment, errorCode, reason, headers);
        } else {
            log.debug("  Failed payment, reason: {}, errorCode: {}, for {}", reason, errorCode, feesData.getVendorRef());
            kafkaSender.sendErrorPayment(feesData.getVendorRef(), new ErrorData(feesData, errorCode, reason), headers);
        }
    }

    @Override
    public void failPayment(FlexcubePayment flexcubePayment, String errorCode, String reason, Headers headers) {
        flexcubePaymentRepository.save(flexcubePayment
                .setErrorCode(errorCode)
                .setReason(reason)
                .setFinishedPayment(true)
                .setUpdatedTime(Instant.now()));
        log.debug("  Failed payment, reason: {}, errorCode: {}, for {}", reason, errorCode, flexcubePayment.getVendorRef());
        kafkaSender.sendErrorPayment(flexcubePayment.getVendorRef(),
                new ErrorData(flexcubePayment.getPendingPayment(), errorCode, reason), headers);
    }

    private FlexcubePayment getFlexcubePayment(String vendorRef) {
        List<FlexcubePayment> payments = flexcubePaymentRepository.findByVendorRef(vendorRef);
        return payments.isEmpty() ? null : payments.get(payments.size() - 1);
    }

    @Override
    public void processRefund(ErrorData errorData) {
        // todo
    }
}
