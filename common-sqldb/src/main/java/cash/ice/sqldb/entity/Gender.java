package cash.ice.sqldb.entity;

public enum Gender {
    MALE, FEMALE;

    public static Gender of(String genderString) {
        if ("M".equals(genderString) || "Male".equals(genderString)) {
            return MALE;
        } else if ("F".equals(genderString) || "Female".equals(genderString)) {
            return FEMALE;
        } else {
            return null;
        }
    }
}
