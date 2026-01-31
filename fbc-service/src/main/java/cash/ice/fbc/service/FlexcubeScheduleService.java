package cash.ice.fbc.service;

import cash.ice.fbc.config.FlexcubeProperties;
import cash.ice.fbc.entity.FlexcubePayment;
import cash.ice.fbc.repository.FlexcubePaymentRepository;
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
public class FlexcubeScheduleService {
    private final FlexcubeProperties flexcubeProperties;
    private final FlexcubePaymentRepository flexcubePaymentRepository;
    private final FlexcubeService flexcubeService;

    @Scheduled(fixedRateString = "${ice.cash.flexcube.status-poll-ms}")
    public void schedulePaymentStatusPollTask() {
        Instant now = Instant.now();
        List<FlexcubePayment> payments = flexcubePaymentRepository.findByFinishedPayment(false);
        payments.forEach(payment -> {
            long durationSeconds = Duration.between(payment.getCreatedTime(), now).getSeconds();
            if (durationSeconds > flexcubeProperties.getStatusPollInitDelay()) {
                try {
                    log.debug("Poll status for {}, duration: {} seconds", payment.getVendorRef(), durationSeconds);
                    flexcubeService.checkStatus(payment);

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }
}
