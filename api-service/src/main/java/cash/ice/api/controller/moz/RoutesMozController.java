package cash.ice.api.controller.moz;

import cash.ice.api.entity.moz.Route;
import cash.ice.api.entity.moz.RouteDetails;
import cash.ice.api.repository.moz.RouteDetailsRepository;
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
public class RoutesMozController {
    private final CountryRepository countryRepository;
    private final RouteRepository routeRepository;
    private final RouteDetailsRepository routeDetailsRepository;

    @QueryMapping
    public Iterable<Route> routesMoz() {
        log.info("> GET moz routes");
        Country country = countryRepository.findByIsoCode("MOZ")
                .orElseThrow(() -> new ICEcashException("Unknown country with 'MOZ' ISO code", EC1065));
        return routeRepository.findByCountryId(country.getId());
    }

    @BatchMapping(typeName = "Route", field = "details")
    public Map<Route, List<RouteDetails>> routeDetails(List<Route> routes) {
        log.info("> GET moz route details");
        return MappingUtil.categoriesToItemsListMap(routes, Route::getId, RouteDetails::getRouteId,
                routeDetailsRepository::findByRouteIdIn);
    }
}
