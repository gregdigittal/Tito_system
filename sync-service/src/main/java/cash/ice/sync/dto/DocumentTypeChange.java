package cash.ice.sync.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class DocumentTypeChange {

    @NotNull(message = "'action' cannot be skipped")
    private ChangeAction action;

    @NotNull(message = "'documentTypeId' cannot be skipped")
    private Integer documentTypeId;

    @NotNull(message = "'accountTypeId' cannot be skipped")
    private Integer accountTypeId;

    private Map<String, Object> data = new HashMap<>();
}
