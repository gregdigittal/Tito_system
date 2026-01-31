package cash.ice.sync.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class DataChange {

    @NotNull(message = "'action' cannot be skipped")
    private ChangeAction action;

    @NotEmpty(message = "'identifier' cannot be skipped")
    private String identifier;

    private Map<String, Object> data = new HashMap<>();
}
