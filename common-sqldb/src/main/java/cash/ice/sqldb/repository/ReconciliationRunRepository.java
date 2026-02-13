package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.ReconciliationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, Integer> {

    List<ReconciliationRun> findByBusinessDateOrderByIdDesc(LocalDate businessDate);
}
