package cash.ice.common.utils;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

@Slf4j
public final class Tool {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(getZimZoneId());
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ALLOWED_RANDOM_LETTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Tool() {
    }

    public static boolean isLess(BigDecimal value1, BigDecimal value2) {
        return value1.compareTo(value2) < 0;
    }

    public static boolean isGreater(BigDecimal value1, BigDecimal value2) {
        return value1.compareTo(value2) > 0;
    }

    public static String moneyRound(BigDecimal value) {
        return value != null ? new DecimalFormat("0.00").format(value) : "0.00";
    }

    public static Map<String, Object> jsonStringToMap(String json) {
        try {
            return (Map<String, Object>) OBJECT_MAPPER.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Map<String, Integer> jsonStringToStringIntMap(String json) {
        try {
            return (Map<String, Integer>) OBJECT_MAPPER.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static List<Map<String, Object>> jsonStringToListMap(String json) {
        try {
            return (List<Map<String, Object>>) OBJECT_MAPPER.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String objectToJsonString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String mapToJsonString(Map<?, ?> map) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(map);
    }

    public static Integer parseInteger(String numStr, Integer defaultValue) {
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static BigDecimal parseBigDecimal(String numStr) {
        try {
            return new BigDecimal(numStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String concat(String... arr) {
        return Arrays.stream(arr).filter(Objects::nonNull).collect(Collectors.joining(""));
    }

    public static String substrFirst(int symbols, String str) {
        return str != null ? str.substring(0, Math.min(str.length(), symbols)) : null;
    }

    public static String substrLast(int symbols, String str) {
        return str != null ? str.substring(str.length() - Math.min(str.length(), symbols)) : null;
    }

    public static <T> void logDifference(String descr, T oldObjectState, T newObjectState, List<String> ignoreFields) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(oldObjectState.getClass());
            Map<String, String> changes = new LinkedHashMap<>();
            for (PropertyDescriptor propertyDesc : beanInfo.getPropertyDescriptors()) {
                String propertyName = propertyDesc.getName();
                Method readMethod = propertyDesc.getReadMethod();
                if (readMethod != null) {
                    Object value = readMethod.invoke(oldObjectState);
                    Object value2 = readMethod.invoke(newObjectState);
                    if (!ignoreFields.contains(propertyName) && !Objects.equals(value, value2)) {
                        changes.put(propertyName, String.format("%s: \"%s\" -> \"%s\"", propertyName, value, value2));
                    }
                }
            }
            if (!changes.isEmpty()) {
                log.info(descr + String.join(", ", changes.values()));
            }
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static long checksum(String str) {
        if (str != null) {
            Checksum checksum = new CRC32();
            checksum.update(str.getBytes());
            return checksum.getValue();
        } else {
            return 0;
        }
    }

    public static BigDecimal correctValueWithin(BigDecimal value, BigDecimal minValue, BigDecimal maxValue) {
        if (minValue != null && value.compareTo(minValue) < 0) {
            return minValue;
        } else {
            return maxValue != null && value.compareTo(maxValue) > 0 ? maxValue : value;
        }
    }

    public static String readResourceAsString(String resourcePath) throws URISyntaxException, IOException {
        URL requestResource = Tool.class.getClassLoader().getResource(resourcePath);
        Objects.requireNonNull(requestResource, String.format("Resource file [%s] is absent!", resourcePath));
        return Files.readString(Paths.get(requestResource.toURI()));
    }

    public static String headerKeys(Headers headers) {
        return Arrays.stream(headers.toArray()).map(Header::key).collect(Collectors.joining(", "));
    }

    public static String headers(Headers headers) {
        return Arrays.stream(headers.toArray()).map(header -> header.key() + "=" +
                new String(header.value())).collect(Collectors.joining(", "));
    }

    public static String fixValidLuhnNumber(String value) {
        int sum = 0;
        int parity = value.length() % 2;
        for (int i = value.length() - 2; i >= 0; i--) {
            int summand = Character.getNumericValue(value.charAt(i));
            if (i % 2 == parity) {
                int product = summand * 2;
                summand = (product > 9) ? (product - 9) : product;
            }
            sum += summand;
        }
        sum %= 10;
        int luhnDigit = sum > 0 ? 10 - sum : sum;
        return value.substring(0, value.length() - 1) + luhnDigit;
    }

    public static String generateDigits(int amount, boolean luhnCheck) {
        return generateDigits(amount, luhnCheck, null);
    }

    public static String generateDigits(int amount, boolean luhnCheck, String prefix) {
        prefix = (prefix != null) ? prefix : "";
        int qty = amount - prefix.length();
        if (qty > 16) {
            throw new IllegalArgumentException("Cannot generate more than 16 digits");
        }
        long minVal = (long) Math.pow(10, qty - 1.0);
        String digits = prefix + RANDOM.longs(minVal, minVal * 10).findFirst().orElseThrow(
                () -> new IllegalStateException(String.format("Cannot generate random %s digits number", amount)));
        return luhnCheck ? fixValidLuhnNumber(digits) : digits;
    }

    public static String generateCharacters(int amount) {
        StringBuilder sb = new StringBuilder(amount);
        for (int i = 0; i < amount; i++) {
            sb.append(ALLOWED_RANDOM_LETTERS.charAt(RANDOM.nextInt(ALLOWED_RANDOM_LETTERS.length())));
        }
        return sb.toString();
    }

    public static boolean isPower10(int number) {
        while (number >= 10 && number % 10 == 0) number /= 10;
        return number == 1;
    }

    public static String truncate(String str, int maxLength) {
        return str != null && str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    public static ZoneId getZimZoneId() {
        return ZoneId.of("Africa/Harare");
    }

    public static String getZimDateTimeString() {
        return getZimDateTimeString(LocalDateTime.now());
    }

    public static String getZimDateTimeString(LocalDateTime localDateTime) {
        return DATE_FORMATTER.format(localDateTime);
    }

    public static LocalDateTime currentDateTime() {
        return LocalDateTime.now(getZimZoneId());
    }

    public static void checkPinIsValid(String pinPassword) {
        if (!pinPassword.matches("[0-9]+")) {
            throw new ICEcashException("Invalid PIN, must contain only numbers", ErrorCodes.EC1045);
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public static List<String> toKafkaHeaderKeys(Headers headers) {
        return headers != null ? Arrays.stream(headers.toArray()).map(Header::key).toList() : null;
    }

    public static Headers toKafkaHeaders(List<String> headerKeys) {
        if (headerKeys != null) {
            List<Header> recordHeaders = headerKeys.stream().<Header>map(h -> new RecordHeader(h, "".getBytes())).toList();
            return new RecordHeaders(recordHeaders);
        } else {
            return null;
        }
    }

    public static <K, V> void remove(Map<K, V> map, List<K> keys) {
        if (map != null) {
            keys.forEach(map::remove);
        }
    }

    public static <K, V> void merge(Map<K, V> mapFrom, Map<K, V> mapTo, List<K> keys) {
        if (mapFrom != null && mapTo != null) {
            keys.forEach(key -> {
                V val = mapFrom.get(key);
                if (val != null) {
                    mapTo.put(key, val);
                }
            });
        }
    }

    public static long size(Iterable<?> collection) {
        return StreamSupport.stream(collection.spliterator(), false).count();
    }

    public static MapBuilder<String, Object> newMetaMap() {
        return new MapBuilder<>(new HashMap<>());
    }

    public static class MapBuilder<K, V> {
        private final Map<K, V> map;

        public MapBuilder(Map<K, V> map) {
            this.map = map;
        }

        public MapBuilder<K, V> put(K key, V val) {
            map.put(key, val);
            return this;
        }

        public MapBuilder<K, V> putAll(Map<K, V> otherMap) {
            map.putAll(otherMap);
            return this;
        }

        public MapBuilder<K, V> putIfNonNull(K key, V val) {
            if (key != null && val != null) {
                map.put(key, val);
            }
            return this;
        }

        public Map<K, V> build() {
            return map;
        }
    }
}
