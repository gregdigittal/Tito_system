package cash.ice.onemoney.service;

import cash.ice.onemoney.config.OnemoneyProperties;
import cash.ice.onemoney.entity.OnemoneyPayment;
import cash.ice.onemoney.error.OnemoneyException;
import cash.ice.onemoney.repository.OnemoneyPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static cash.ice.common.error.ErrorCodes.EC7004;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnemoneyScheduleService {
    private final OnemoneyProperties onemoneyProperties;
    private final OnemoneyPaymentRepository onemoneyPaymentRepository;
    private final OnemoneyPaymentService onemoneyPaymentService;

    @Scheduled(fixedRateString = "${ice.cash.onemoney.status-poll-check-ms}")
    public void schedulePaymentStatusPollTask() {
        Instant now = Instant.now();
        List<OnemoneyPayment> payments = onemoneyPaymentRepository.findByNeedCheckStatus(Boolean.TRUE);
        payments.forEach(payment -> {
            long durationSeconds = Duration.between(payment.getCreatedTime(), now).getSeconds();
            if (durationSeconds > onemoneyPaymentService.getStatusPollInitDelay(payment)) {
                try {
                    log.debug("Poll status for {}, duration: {} seconds", payment.getVendorRef(), durationSeconds);
                    payment.setNeedCheckStatus(null);
                    onemoneyPaymentService.checkStatus(payment, durationSeconds, false);

                } catch (OnemoneyException e) {
                    onemoneyPaymentService.failPayment(payment, e.getErrorCode(), e.getMessage(), null);

                } catch (Exception e) {
                    onemoneyPaymentService.failPayment(payment, EC7004, e.getMessage(), null);
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

    @Scheduled(fixedRateString = "${ice.cash.onemoney.expired-payments-recheck-ms}")
    public void schedulePaymentRecheckTask() {
        Instant now = Instant.now();
        List<OnemoneyPayment> payments = onemoneyPaymentRepository.findByNeedRecheckStatus(Boolean.TRUE);
        payments.forEach(payment -> {
            long durationSeconds = Duration.between(payment.getCreatedTime(), now).getSeconds();
            if (durationSeconds > onemoneyPaymentService.getExpiredPaymentsRecheckAfterTime(payment)) {
                try {
                    log.debug("Recheck status for {}, duration: {} seconds", payment.getVendorRef(), durationSeconds);
                    payment.setNeedRecheckStatus(null);
                    onemoneyPaymentService.checkStatus(payment, durationSeconds, true);

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    onemoneyPaymentRepository.save(payment);
                }
            }
        });
    }

    @Scheduled(cron = "${ice.cash.onemoney.cleanup-history-cron}")
    public void scheduleCleanupHistoryTask() {
        Instant startDate = Instant.now().atZone(ZoneId.systemDefault())
                .minusDays(onemoneyProperties.getCleanupHistoryOlderDays()).toInstant();
        List<OnemoneyPayment> oldPayments = onemoneyPaymentRepository.findAllByCreatedTimeIsBefore(startDate);
        log.info("Cleanup history older than: {}, removing {} old payments", startDate, oldPayments.size());
        onemoneyPaymentRepository.deleteAll(oldPayments);
    }
}
