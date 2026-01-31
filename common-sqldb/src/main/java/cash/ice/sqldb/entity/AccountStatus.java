package cash.ice.sqldb.entity;

public enum AccountStatus {
    ACTIVE, FROZEN;

    public static AccountStatus of(boolean isActive) {
        return isActive ? ACTIVE : FROZEN;
    }
}
