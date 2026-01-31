package cash.ice.paygo.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class Credential extends CredentialCreate {
    @NotBlank
    private String id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String merchantCredentialId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String created;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String updated;
}
