package cash.ice.api.repository.moz;

import cash.ice.api.entity.moz.RouteDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteDetailsRepository extends JpaRepository<RouteDetails, Integer> {

    List<RouteDetails> findByRouteIdIn(List<Integer> routeIds);
}