package cash.ice.api.repository.moz;

import cash.ice.api.entity.moz.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Integer> {

    List<Route> findByCountryId(Integer countryId);
}