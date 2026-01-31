package cash.ice.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Objects;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class LoginEntityRequest {

    @NotEmpty(message = "username must not be empty")
    private String username;
    @NotEmpty(message = "password must not be empty")
    @Size(min = 4, max = 4, message = "PIN must be 4 digits only")
    private String password;
    private String clientId;
    private String clientSecret;
    private String grantType;
    private String scope;

    public void setClient_id(String clientId) {
        this.clientId = clientId;
    }

    public void setClient_secret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setGrant_type(String grantType) {
        this.grantType = grantType;
    }

    @Override
    public String toString() {
        return "LoginUserRequest(" +
                "username=" + username +
                ", password=" + (Objects.isNull(password) ? null : "*".repeat(password.length())) +
                ", clientId=" + clientId +
                ", clientSecret=" + clientSecret +
                ", grantType=" + grantType +
                ", scope=" + scope +
                ')';
    }
}
