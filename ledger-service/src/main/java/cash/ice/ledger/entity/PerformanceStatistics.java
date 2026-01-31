package cash.ice.ledger.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "perf_statistics")
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PerformanceStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "info", nullable = false)
    private String info;

    @Column(name = "total_records", nullable = false)
    private Integer totalRecords;

    @Column(name = "account_records", nullable = false)
    private Integer accountRecords;

    @Column(name = "min")
    private BigDecimal min;

    @Column(name = "avg")
    private BigDecimal avg;

    @Column(name = "max")
    private BigDecimal max;

    @Column(name = "total_duration")
    private BigDecimal totalDuration;

    @Column(name = "real_duration")
    private BigDecimal realDuration;

    @Column(name = "parallelism")
    private Integer parallelism;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

}
