package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.SweepRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SweepRuleRepository extends JpaRepository<SweepRule, Integer> {

    List<SweepRule> findByAccountIdAndActiveTrue(Integer accountId);
}
