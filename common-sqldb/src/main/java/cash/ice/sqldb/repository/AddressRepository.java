package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Address;
import cash.ice.sqldb.entity.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    Optional<Address> findByEntityIdAndAddressType(Integer entityId, AddressType addressType);

    List<Address> findByEntityIdIn(List<Integer> entityIds);
}
