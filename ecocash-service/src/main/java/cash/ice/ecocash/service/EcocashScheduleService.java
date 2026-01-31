package cash.ice.ecocash.service;

import cash.ice.ecocash.config.EcocashProperties;
import cash.ice.ecocash.entity.EcocashPayment;
import cash.ice.ecocash.repository.EcocashPaymentRepository;
import error.EcocashException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static cash.ice.common.error.ErrorCodes.EC6001;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcocashScheduleService {
    private final EcocashProperties ecocashProperties;
    private final EcocashPaymentRepository ecocashPaymentRepository;
    private final EcocashPaymentService ecocashPaymentService;

    @Scheduled(fixedRateString = "${ice.cash.ecocash.status-poll-ms}")
    public void schedulePaymentStatusPollTask() {
        Instant now = Instant.now();
        List<EcocashPayment> payments = ecocashPaymentRepository.findByFinishedPayment(false);
        payments.forEach(ecocashPayment -> {
            long durationSeconds = Duration.between(ecocashPayment.getCreatedTime(), now).getSeconds();
            if (durationSeconds > ecocashPaymentService.getStatusPollInitDelay(ecocashPayment)) {
                try {
                    log.debug("Check status for {}, duration: {} seconds", ecocashPayment.getVendorRef(), durationSeconds);
                    ecocashPaymentService.checkStatus(ecocashPayment, durationSeconds);

                } catch (EcocashException e) {
                    ecocashPaymentService.processError(e);

                } catch (Exception e) {
                    ecocashPaymentService.processError(new EcocashException(ecocashPayment, e.getMessage(), EC6001, e));
                }
            }
        });
    }

    @Scheduled(fixedRateString = "${ice.cash.ecocash.expired-payments-recheck-ms}")
    public void schedulePaymentRecheckTask() {
        Instant now = Instant.now();
        List<EcocashPayment> payments = ecocashPaymentRepository.findByRecheck(true);
        payments.forEach(ecocashPayment -> {
            long durationSeconds = Duration.between(ecocashPayment.getCreatedTime(), now).getSeconds();
            if (durationSeconds > ecocashProperties.getExpiredPaymentsRecheckAfter()) {
                try {
                    log.debug("Recheck status for {}, duration: {} seconds", ecocashPayment.getVendorRef(), durationSeconds);
                    ecocashPayment.setRecheck(null);
                    ecocashPaymentService.recheckStatus(ecocashPayment);

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    ecocashPaymentRepository.save(ecocashPayment);
                }
            }
        });
    }

    @Scheduled(cron = "${ice.cash.ecocash.cleanup-history-cron}")
    public void scheduleCleanupHistoryTask() {
        Instant startDate = Instant.now().atZone(ZoneId.systemDefault())
                .minusDays(ecocashProperties.getCleanupHistoryOlderDays()).toInstant();
        List<EcocashPayment> oldPayments = ecocashPaymentRepository.findAllByCreatedTimeIsBefore(startDate);
        log.info("Cleanup history older than: {}, removing {} old payments", startDate, oldPayments.size());
        ecocashPaymentRepository.deleteAll(oldPayments);
    }
}
