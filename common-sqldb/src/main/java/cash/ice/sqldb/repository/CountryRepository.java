package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Integer> {

    Optional<Country> findByIsoCode(String isoCode);
}
