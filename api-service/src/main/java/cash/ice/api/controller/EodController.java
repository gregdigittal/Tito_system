package cash.ice.api.controller;

import cash.ice.api.service.EodSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * Phase 8-11: Admin trigger for EOD settlement. Staff/finance admin only.
 */
@RestController
@RequestMapping("/api/v1/eod")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class EodController {

    private final EodSettlementService eodSettlementService;

    /**
     * Run EOD (settlement → fee → sweep) for the given business date.
     * Query param date: optional, default yesterday (ISO date e.g. 2025-01-30).
     */
    @PostMapping(value = "/run", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> runEod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate businessDate = date != null ? date : LocalDate.now().minusDays(1);
        log.info("EOD run triggered via API for business date {}", businessDate);
        eodSettlementService.runForDate(businessDate);
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "businessDate", businessDate.toString(),
                "message", "EOD settlement completed for " + businessDate));
    }
}
