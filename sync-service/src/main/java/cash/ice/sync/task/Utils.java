package cash.ice.sync.task;

import org.springframework.util.ObjectUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public final class Utils {

    private Utils() {
    }

    public static <K, V> V getVal(Map<K, V> map, K id) {
        return ObjectUtils.isEmpty(id) ? null : map.get(id);
    }

    public static <K, V> V getValNotNull(Map<K, V> map, K id, String label) {
        if (ObjectUtils.isEmpty(id) || !map.containsKey(id)) {
            throw new IllegalArgumentException(String.format("Unknown %s: %s", label, id));
        }
        return map.get(id);
    }

    public static String getString(ResultSet resultSet, String columnName) throws SQLException {
        String value = resultSet.getString(columnName);
        return ObjectUtils.isEmpty(value) ? null : value;
    }

    public static Integer getInt(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getObject(columnName, Integer.class);
    }

    public static boolean containsKey(Map<String, Object> map, List<String> keys) {
        return keys.stream().anyMatch(map::containsKey);
    }
}
