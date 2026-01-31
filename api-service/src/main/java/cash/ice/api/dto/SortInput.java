package cash.ice.api.dto;

import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Data
public class SortInput {
    private List<String> asc;
    private List<String> desc;
    private List<SortProperty> by;

    public static Sort toSort(SortInput sortInput) {
        return toSort(sortInput, Map.of());
    }

    public static Sort toSort(SortInput sortInput, Map<String, String> fieldsRewriterMap) {
        Function<String, String> stringRewriterFunction = f -> fieldsRewriterMap.getOrDefault(f, f);
        if (sortInput == null || CollectionUtils.isEmpty(sortInput.asc) && CollectionUtils.isEmpty(sortInput.desc)
                && CollectionUtils.isEmpty(sortInput.by)) {
            return Sort.unsorted();

        } else if (!CollectionUtils.isEmpty(sortInput.asc)) {
            return Sort.by(Sort.Direction.ASC, sortInput.asc.stream().map(stringRewriterFunction).toArray(String[]::new));

        } else if (!CollectionUtils.isEmpty(sortInput.desc)) {
            return Sort.by(Sort.Direction.DESC, sortInput.desc.stream().map(stringRewriterFunction).toArray(String[]::new));

        } else {
            return Sort.by(sortInput.by.stream().map(o -> new Sort.Order(o.dir, stringRewriterFunction.apply(o.prop))).toList());
        }
    }

    @Override
    public String toString() {
        return "SortInput{" +
                (asc == null ? "" : " asc=" + asc) +
                (desc == null ? "" : " desc=" + desc) +
                (by == null ? "" : " by=" + by) +
                '}';
    }

    @Data
    public static class SortProperty {
        private String prop;
        private Sort.Direction dir;
    }
}
