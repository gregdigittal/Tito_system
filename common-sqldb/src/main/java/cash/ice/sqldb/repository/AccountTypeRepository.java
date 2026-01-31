package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountTypeRepository extends JpaRepository<AccountType, Integer> {

    Optional<AccountType> findByNameAndCurrencyId(String name, int currencyId);

    Optional<AccountType> findByLegacyWalletId(String legacyWalletId);
}