package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.SecurityGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SecurityGroupRepository extends JpaRepository<SecurityGroup, Integer> {

    Optional<SecurityGroup> findByLegacyId(String legacyId);

    List<SecurityGroup> findByRightsName(String name);
}