package cash.ice.common.util;

/**
 * SQL-related utilities. Use when passing user input into LIKE clauses
 * to prevent wildcard injection (% and _).
 */
public final class SqlUtil {

    private SqlUtil() {
    }

    /**
     * Escapes LIKE special characters (\, %, _) so the value is treated literally.
     * Use with ESCAPE '\\' in the SQL (e.g. ... LIKE :param ESCAPE '\\').
     *
     * @param value user-supplied string, may be null
     * @return escaped string, or null if value is null
     */
    public static String escapeLikeParam(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
