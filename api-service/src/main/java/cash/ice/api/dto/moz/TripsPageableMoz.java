package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/** Phase 8-4: Page of trips for GraphQL type TripsPageableMoz. */
@Data
@Accessors(chain = true)
public class TripsPageableMoz {
    private long total;
    private List<TripMoz> content;
}
