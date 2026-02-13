package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.SettlementRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRuleRepository extends JpaRepository<SettlementRule, Integer> {

    List<SettlementRule> findByEntityIdAndActiveTrue(Integer entityId);
}
