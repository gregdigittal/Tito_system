package cash.ice.api.controller.ken;

import cash.ice.api.entity.ken.RouteFares;
import cash.ice.api.entity.moz.Route;
import cash.ice.api.repository.ken.RouteFaresRepository;
import cash.ice.api.repository.moz.RouteRepository;
import cash.ice.api.util.MappingUtil;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.Country;
import cash.ice.sqldb.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

import static cash.ice.common.error.ErrorCodes.EC1065;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RoutesKenController {
    private final CountryRepository countryRepository;
    private final RouteRepository routeRepository;
    private final RouteFaresRepository routeFaresRepository;

    @QueryMapping
    public Iterable<Route> routesKen() {
        log.info("> GET ken routes");
        Country country = countryRepository.findByIsoCode("KEN")
                .orElseThrow(() -> new ICEcashException("Unknown country with 'KEN' ISO code", EC1065));
        return routeRepository.findByCountryId(country.getId());
    }

    @BatchMapping(typeName = "RouteKen", field = "fares")
    public Map<Route, List<RouteFares>> routeFares(List<Route> routes) {
        log.info("> GET ken route fares");
        return MappingUtil.categoriesToItemsListMap(routes, Route::getId, RouteFares::getRouteId,
                routeFaresRepository::findByRouteIdIn);
    }
}
