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
}
