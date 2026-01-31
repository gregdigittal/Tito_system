package cash.ice.sqldb.repository.zim;

import cash.ice.sqldb.entity.zim.BankBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankBranchRepository extends JpaRepository<BankBranch, Integer> {

    Optional<BankBranch> findByLegacyBankId(int legacyBankId);

    Optional<BankBranch> findByBankIdAndName(int bankId, String name);

    Optional<BankBranch> findByBankIdAndBranchNoAndName(int bankId, int branchNo, String name);
}
