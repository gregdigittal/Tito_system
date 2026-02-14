package cash.ice.api.controller;

import cash.ice.api.dto.ReconciliationRunDto;
import cash.ice.sqldb.entity.ReconciliationRun;
import cash.ice.sqldb.repository.ReconciliationRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 8-13: Admin read of reconciliation runs. Staff/finance admin only.
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class ReconciliationController {

    private final ReconciliationRunRepository reconciliationRunRepository;

    /**
     * List reconciliation runs, optionally filtered by business date.
     * Query param date: optional (ISO date). If present, returns runs for that date only.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReconciliationRunDto>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ReconciliationRun> runs = date != null
                ? reconciliationRunRepository.findByBusinessDateOrderByIdDesc(date)
                : reconciliationRunRepository.findAll(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "id"))).getContent();
        List<ReconciliationRunDto> dtos = runs.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private ReconciliationRunDto toDto(ReconciliationRun r) {
        return ReconciliationRunDto.builder()
                .id(r.getId())
                .businessDate(r.getBusinessDate())
                .deviceId(r.getDeviceId())
                .startedAt(r.getStartedAt())
                .finishedAt(r.getFinishedAt())
                .status(r.getStatus())
                .serverCount(r.getServerCount())
                .deviceCount(r.getDeviceCount())
                .matchedCount(r.getMatchedCount())
                .mismatchCount(r.getMismatchCount())
                .detailJson(r.getDetailJson())
                .build();
    }
}
