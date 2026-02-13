package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Trip;
import cash.ice.sqldb.entity.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Integer> {

    Page<Trip> findByEntityIdOrderByTapInAtDesc(Integer entityId, Pageable pageable);

    Optional<Trip> findFirstByEntityIdAndStatusOrderByTapInAtDesc(Integer entityId, TripStatus status);
}
