package cash.ice.zim.api.service;

import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.zim.api.config.ZimApiProperties;
import cash.ice.zim.api.dto.PaymentResponseZim;
import cash.ice.zim.api.dto.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZimPaymentScheduleService {
    private final ZimPaymentService paymentService;
    private final ZimLoggerService loggerService;
    private final ZimApiProperties zimApiProperties;

    @Scheduled(fixedRateString = "${ice.cash.zim.api.sp-poll-scheduler-ms}")
    public void schedulePaymentStatusPollTask() {
        Instant now = Instant.now();
        List<PaymentResponseZim> payments = loggerService.getResponsesBy(ResponseStatus.APPROVE_FAILED, null, null,
                null, null, null, null, null, null, null, null, PaymentResponseZim.class);
        if (payments != null && !payments.isEmpty()) {
            log.debug("-found {} payments to poll SP", payments.size());
            payments.forEach(payment -> {
                long durationMillis = Duration.between(payment.getLastSpTry(), now).toMillis();
                if (durationMillis > zimApiProperties.getSpTriesInterval().toMillis()) {
                    try {
                        log.debug("-Approving payment vendorRef: {}, durationMs: {}", payment.getVendorRef(), durationMillis);
                        PaymentRequestZim paymentRequest = loggerService.getRequest(payment.getVendorRef(), PaymentRequestZim.class);
                        paymentService.spPollingForSuccessPayment(paymentRequest, payment);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
    }
}
