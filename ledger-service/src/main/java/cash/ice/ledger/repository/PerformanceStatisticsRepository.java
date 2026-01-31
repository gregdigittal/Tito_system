package cash.ice.ledger.repository;

import cash.ice.ledger.entity.PerformanceStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceStatisticsRepository extends JpaRepository<PerformanceStatistics, Integer> {
}
