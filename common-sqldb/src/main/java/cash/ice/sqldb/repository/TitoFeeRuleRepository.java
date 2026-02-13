package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.TitoFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TitoFeeRuleRepository extends JpaRepository<TitoFeeRule, Integer> {

    List<TitoFeeRule> findByCountryCodeAndActiveTrue(String countryCode);
}
