package cash.ice.api.documentation;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ErrorDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.snippet.TemplatedSnippet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class IcecashErrorCodesDocumentation extends TemplatedSnippet {

    protected IcecashErrorCodesDocumentation() {
        this(null);
    }

    protected IcecashErrorCodesDocumentation(Map<String, Object> attributes) {
        super("error-codes", attributes);
    }

    public static IcecashErrorCodesDocumentation errorCodes() {
        return new IcecashErrorCodesDocumentation();
    }

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = new HashMap<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        model.put("errors", errors);

        for (Field field : ErrorCodes.class.getFields()) {
            if (field.getType() == String.class) {
                try {
                    errors.add(Map.of(
                            "service", getErrorService(field),
                            "code", field.get(null),
                            "description", getErrorDescription(field)));
                } catch (IllegalAccessException e) {
                    log.warn("Cannot get access to field: " + field);
                }
            }
        }
        return model;
    }

    private String getErrorService(Field field) {
        if (field.isAnnotationPresent(ErrorDescription.class)) {
            return field.getAnnotation(ErrorDescription.class).service();
        }
        return "";
    }

    private String getErrorDescription(Field field) {
        if (field.isAnnotationPresent(ErrorDescription.class)) {
            return field.getAnnotation(ErrorDescription.class).descr();
        }
        return "";
    }
}
