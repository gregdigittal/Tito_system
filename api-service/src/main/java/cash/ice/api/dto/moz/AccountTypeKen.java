package cash.ice.api.dto.moz;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static cash.ice.sqldb.entity.EntityType.*;

@Getter
@RequiredArgsConstructor
public enum AccountTypeKen {
    Farmer(FNDS_FARMER, 2001),
    AgriDealer(FNDS_AGRI_DEALER, 2002),
    Agent(FNDS_AGENT, 2003);

    private final String entityType;
    private final int securityGroupId;
}
