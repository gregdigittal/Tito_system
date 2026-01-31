package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Integer> {

    Optional<Currency> findByIsoCode(String isoCode);
}