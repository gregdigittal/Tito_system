package cash.ice.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserAuthRequest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String keycloakId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String username;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String number;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String pvv;

    private String firstName;
    private String lastName;
    private String email;
}
