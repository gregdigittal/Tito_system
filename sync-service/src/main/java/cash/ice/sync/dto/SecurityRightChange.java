package cash.ice.sync.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class SecurityRightChange {

    @NotNull(message = "'action' cannot be skipped")
    private ChangeAction action;

    @NotNull(message = "'right' cannot be skipped")
    private String right;

    private Map<String, Object> data = new HashMap<>();
}
