package cash.ice.sync.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class SecurityGroupChange {

    @NotNull(message = "'action' cannot be skipped")
    private ChangeAction action;

    @NotNull(message = "'identifier' cannot be skipped")
    private Integer identifier;

    @NotNull(message = "'type' must be either 'mobi' or 'online'")
    private String type;

    private Map<String, Object> data = new HashMap<>();
}
