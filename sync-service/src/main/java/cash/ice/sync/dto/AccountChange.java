package cash.ice.sync.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class AccountChange {

    @NotNull(message = "'action' cannot be skipped")
    private ChangeAction action;

    @NotNull(message = "'legacyAccountId' cannot be skipped")
    private Integer legacyAccountId;

    @NotNull(message = "'legacyWalletId' cannot be skipped")
    private Integer legacyWalletId;

    private Map<String, Object> data = new HashMap<>();
}
