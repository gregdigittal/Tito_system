package cash.ice.sqldb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Phase 8-13: Daily reconciliation run — summary per business date / device.
 */
@Entity
@Table(name = "reconciliation_run")
@Data
@Accessors(chain = true)
public class ReconciliationRun implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "server_count")
    private Integer serverCount = 0;

    @Column(name = "device_count")
    private Integer deviceCount = 0;

    @Column(name = "matched_count")
    private Integer matchedCount = 0;

    @Column(name = "mismatch_count")
    private Integer mismatchCount = 0;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;
}
