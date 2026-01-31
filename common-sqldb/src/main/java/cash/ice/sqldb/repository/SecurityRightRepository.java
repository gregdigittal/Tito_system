package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.SecurityRight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecurityRightRepository extends JpaRepository<SecurityRight, Integer> {

    Optional<SecurityRight> findByName(String name);
}