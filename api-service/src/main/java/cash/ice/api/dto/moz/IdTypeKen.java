package cash.ice.api.dto.moz;

import lombok.Getter;

@Getter
public enum IdTypeKen {
    NationalID(101),
    Passport(102);

    private final int dbId;

    IdTypeKen(int dbId) {
        this.dbId = dbId;
    }
}
