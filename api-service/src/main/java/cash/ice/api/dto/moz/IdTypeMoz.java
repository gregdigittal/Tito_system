package cash.ice.api.dto.moz;

import lombok.Getter;

@Getter
public enum IdTypeMoz {
    ID(4),
    Passport(5),
    DIRE(6),
    NUEL(7);

    private final int dbId;

    IdTypeMoz(int dbId) {
        this.dbId = dbId;
    }
}
