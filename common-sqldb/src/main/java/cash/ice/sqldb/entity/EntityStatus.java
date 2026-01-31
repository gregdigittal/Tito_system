package cash.ice.sqldb.entity;

public enum EntityStatus {
    ACTIVE, FROZEN;

    public static EntityStatus of(boolean isActive) {
        return isActive ? ACTIVE : FROZEN;
    }
}
