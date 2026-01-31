package cash.ice.api.repository.ken;

import cash.ice.api.entity.ken.RouteFares;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteFaresRepository extends JpaRepository<RouteFares, Integer> {

    List<RouteFares> findByRouteIdIn(List<Integer> routeIds);
}