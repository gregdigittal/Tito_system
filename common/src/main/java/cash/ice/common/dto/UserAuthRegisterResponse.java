package cash.ice.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserAuthRegisterResponse {
    private String keycloakId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer totalUsersCount;
}
