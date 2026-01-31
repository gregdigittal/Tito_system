package cash.ice;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Accessors(chain = true)
public class Wrapper {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{.*?}");

    private final Object data;

    public Wrapper print(String comment) {
        System.out.printf("%s: %s%n", comment, data.toString());
        return this;
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public String getStr() {
        return (String) data;
    }

    public String getStr(String path, String... placeholders) {
        return (String) getObject(path, placeholders);
    }

    public Integer getInt() {
        return (Integer) data;
    }

    public Integer getInt(String path, String... placeholders) {
//        return (Integer) getObject(path, placeholders);
        Object result = getObject(path, placeholders);
        return (result instanceof String s) ? Integer.parseInt(s) : (Integer) result;
    }

    public Double getDouble() {
        return (Double) data;
    }

    public Double getDbl(String path, String... placeholders) {
        return (Double) getObject(path, placeholders);
    }

    public Boolean getBool() {
        return (Boolean) data;
    }

    public Boolean toBool(String path, String... placeholders) {
        return (Boolean) getObject(path, placeholders);
    }

    public String toJsonString(List<String> fields, Map<String, String> aliases) {
        if (data instanceof Map<?, ?> map) {
            return mapToJsonString((Map<String, ?>) map, fields, aliases);
        } else if (data instanceof List<?> list) {
            return list.stream().map(m -> mapToJsonString((Map<String, ?>) m, fields, aliases)).collect(Collectors.joining(", ", "[", "]"));
        } else {
            throw new RuntimeException("Cannot make JSON string from primitive type");
        }
    }

    private String mapToJsonString(Map<String, ?> map, List<String> fieldsList, Map<String, String> aliases) {
        return map.entrySet().stream().filter(e -> fieldsList.contains(e.getKey())).map(e -> Map.entry(aliases.getOrDefault(e.getKey(), e.getKey()), e.getValue())).map(e ->
                String.format("%s: %s", e.getKey(), e.getValue())).collect(Collectors.joining(", ", "{", "}"));
    }

    public Wrapper toWrapper(String path, String... placeholders) {
        return new Wrapper(getObject(path, placeholders));
    }

    public List<Wrapper> toWrapperList() {
        return ((List<?>) data).stream().map(Wrapper::new).toList();
    }

    public List<Wrapper> toWrapperList(String path, String... placeholders) {                             // todo recheck places
        List<Map<String, Object>> listOfMaps = (List<Map<String, Object>>) getObject(path, placeholders);
        return listOfMaps.stream().map(Wrapper::new).toList();
    }

    public int getListSize(String path, String... placeholders) {
        return ((List<Object>) getObject(path, placeholders)).size();
    }

    public List<Object> getObjectList(String path, String... placeholders) {
        return (List<Object>) getObject(path, placeholders);
    }

    public List<String> getStrList() {
        return (List<String>) data;
    }

    public List<String> getStrList(String path, String... placeholders) {
        return (List<String>) getObject(path, placeholders);
    }

    public Object getObject(String path, String... placeholders) {
        return getObject(usePlaceholdersIfNeed(path, placeholders), data);
    }

    private String usePlaceholdersIfNeed(String path, String[] placeholders) {
        if (placeholders != null && placeholders.length > 0) {
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(path);
            for (String placeholder : placeholders) {
                if (matcher.find()) {
                    path = path.replaceFirst(PLACEHOLDER_PATTERN.pattern(), placeholder);
                } else {
                    throw new RuntimeException(String.format("Too many placeholders provided for path: '%s', placeholders: %s", path, Arrays.toString(placeholders)));
                }
            }
            if (matcher.find()) {
                throw new RuntimeException(String.format("Not enough placeholders provided for path: '%s', placeholders: %s", path, Arrays.toString(placeholders)));
            }
        }
        return path;
    }

    private Object getObject(String path, Object data) {
        path = path.strip();
        int ind1, ind2 = -1;
        while (ind2 < path.length() - 1) {
            ind1 = (ind2 > 0 && path.charAt(ind2) == ']') ? findNextStop(path, ind2) : ind2;
            ind2 = findNextStop(path, ind1);
            String part = path.substring(ind1 + (path.charAt(ind1 + 1) == '[' ? 2 : 1), ind2 > 0 && ind2 < path.length() ? ind2 : path.length()).strip();

            if (data instanceof Map<?, ?> map) {
                data = map.get(part);

            } else if (data instanceof List<?> list) {
                if (part.contains("=")) {                       // search in list
                    String[] operands = part.split("=");
                    List<?> result = list.stream().filter(acc -> Objects.equals(getObject(operands[0], acc), operands[1].strip())).toList();
                    data = result.size() == 1 ? result.getFirst() : result;
                } else if ("last".equals(part)) {
                    data = list.getLast();
                } else if ("first".equals(part)) {
                    data = list.getFirst();
                } else if (part.matches("-?\\d+")) {
                    int index = Integer.parseInt(part);
                    data = (index < 0) ? list.get(list.size() + index) : list.get(index);
                } else {
                    throw new RuntimeException(String.format("Cannot get '%s' at %s (%s) from list value: %s", part, ind1 + 1, path, data));
                }
            } else {
                throw new RuntimeException(String.format("Cannot get '%s' from primitive value: %s", part, data));
            }
        }
        return data;
    }

    private int findNextStop(String path, int prevIndex) {
        if (prevIndex < 0) {
            prevIndex = 0;
        }
        if (path.charAt(prevIndex) == '[') {
            int brackets = 1;
            for (int i = prevIndex + 1; i < path.length(); i++) {
                if (path.charAt(i) == '[') {
                    brackets++;
                } else if (path.charAt(i) == ']') {
                    brackets--;
                    if (brackets == 0) {
                        return i;
                    }
                }
            }
            throw new RuntimeException(String.format("No closing bracket for: '%s' in '%s'.%s", path.substring(0, prevIndex + 1),
                    path.substring(prevIndex + 1), (brackets > 0 ? " Found " + brackets + " unclosed brackets." : "")));
        } else {
            for (int i = prevIndex + 1; i < path.length(); i++) {
                if (path.charAt(i) == '.' || path.charAt(i) == '[') {
                    return i;
                }
            }
            return path.length() + 1;
        }
    }
}
