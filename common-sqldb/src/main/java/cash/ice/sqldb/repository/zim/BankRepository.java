package cash.ice.sqldb.repository.zim;

import cash.ice.sqldb.entity.zim.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankRepository extends JpaRepository<Bank, Integer> {

    Optional<Bank> findByName(String name);
}
