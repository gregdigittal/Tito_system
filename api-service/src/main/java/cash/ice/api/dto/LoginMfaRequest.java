package cash.ice.api.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Objects;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class LoginMfaRequest {

    @NotEmpty
    private String username;
    @NotEmpty
    private String code;

    @Override
    public String toString() {
        return "LoginMfaRequest{" +
                "username='" + username + '\'' +
                ", code='" + (Objects.isNull(code) ? null : "*".repeat(code.length())) + '\'' +
                '}';
    }
}
