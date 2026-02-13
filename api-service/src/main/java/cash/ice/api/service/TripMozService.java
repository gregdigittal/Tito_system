package cash.ice.api.service;

import cash.ice.api.dto.moz.TripMoz;
import cash.ice.api.dto.moz.TripsPageableMoz;

/** Phase 8-4: User trips (tap-in/tap-out) for commuter. */
public interface TripMozService {

    TripsPageableMoz getTrips(Integer entityId, int page, int size);

    TripMoz getCurrentTrip(Integer entityId);
}
