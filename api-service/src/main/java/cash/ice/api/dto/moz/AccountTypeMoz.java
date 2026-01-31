package cash.ice.api.dto.moz;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountTypeMoz {
    CommuterRegular("Commuter", 1013),
    CommuterElders("Commuter", 1013),
    CommuterDisabled("Commuter", 1013),
    CommuterStudent("Commuter", 1013),
    CommuterWarVeteran("Commuter", 1013),

    TransportOwnerPrivate("TransportOwner", 1014),
    TransportOwnerPublic("TransportOwner", 1014),

    AgentRegular("Agent", 1011),
    AgentFematro("Agent", 1012),

    TaxiDriver("TaxiDriver", 1015),

    FareCollector("FareCollector", 1016);

    private final String typeString;
    private final int securityGroupId;
}
