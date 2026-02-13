package cash.ice.api.service.impl;

import cash.ice.api.dto.moz.TripMoz;
import cash.ice.api.dto.moz.TripsPageableMoz;
import cash.ice.api.service.TripMozService;
import cash.ice.sqldb.entity.Trip;
import cash.ice.sqldb.entity.TripStatus;
import cash.ice.sqldb.repository.CurrencyRepository;
import cash.ice.sqldb.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripMozServiceImpl implements TripMozService {

    private final TripRepository tripRepository;
    private final CurrencyRepository currencyRepository;

    @Override
    public TripsPageableMoz getTrips(Integer entityId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var p = tripRepository.findByEntityIdOrderByTapInAtDesc(entityId, pageable);
        List<TripMoz> content = p.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new TripsPageableMoz().setTotal(p.getTotalElements()).setContent(content);
    }

    @Override
    public TripMoz getCurrentTrip(Integer entityId) {
        return tripRepository.findFirstByEntityIdAndStatusOrderByTapInAtDesc(entityId, TripStatus.ACTIVE)
                .map(this::toDto)
                .orElse(null);
    }

    private TripMoz toDto(Trip t) {
        String currency = null;
        if (t.getCurrencyId() != null) {
            currency = currencyRepository.findById(t.getCurrencyId())
                    .map(c -> c.getIsoCode())
                    .orElse(null);
        }
        return new TripMoz()
                .setId(t.getId())
                .setTapInAt(t.getTapInAt())
                .setTapOutAt(t.getTapOutAt())
                .setRouteId(t.getRouteId())
                .setRouteName(t.getRouteName())
                .setFare(t.getFare())
                .setCurrency(currency)
                .setStatus(t.getStatus() != null ? t.getStatus().name() : null)
                .setDeviceId(t.getDeviceId());
    }
}
