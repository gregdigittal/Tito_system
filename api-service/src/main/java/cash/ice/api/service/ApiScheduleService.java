package cash.ice.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiScheduleService {
    private final MfaService mfaService;
    private final Me60MozService me60MozService;
    private final OtpService otpService;
    private final EodSettlementService eodSettlementService;
    private final ReconciliationService reconciliationService;

    /** Phase 8-11: EOD settlement (settlement → fee → sweep). Default 02:00 daily. Set ice.cash.eod.settlement.cron to disable (e.g. "-"). */
    @Scheduled(cron = "${ice.cash.eod.settlement.cron:0 0 2 * * ?}")
    public void scheduleEodSettlement() {
        try {
            eodSettlementService.runForDate(java.time.LocalDate.now().minusDays(1));
        } catch (Exception e) {
            log.error("EOD settlement job failed", e);
        }
    }

    @Scheduled(cron = "${ice.cash.staff.mfa.expired-tokens-cleanup-cron}")
    public void scheduleCleanupExpiredTokensTask() {
        mfaService.cleanupExpiredTokensTask();
    }

    @Scheduled(cron = "${ice.cash.moz.link-tag-expired-requests-cleanup-cron}")
    public void scheduleCleanupExpiredMozLinkTagTask() {
        me60MozService.cleanupExpiredMozLinkTagTask();
    }

    @Scheduled(cron = "${ice.cash.otp.expired-requests-cleanup-cron}")
    public void scheduleCleanupExpiredOtpDataTask() {
        otpService.cleanupExpiredOtpDataTask();
    }

    /** Phase 8-13: Daily reconciliation (server vs devices). Default 03:00 daily. */
    @Scheduled(cron = "${ice.cash.reconciliation.cron:0 0 3 * * ?}")
    public void scheduleReconciliation() {
        try {
            reconciliationService.runForDate(java.time.LocalDate.now().minusDays(1));
        } catch (Exception e) {
            log.error("Reconciliation job failed", e);
        }
    }
}
