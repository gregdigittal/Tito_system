package cash.ice.paygo.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class CredentialCreate {
    @NotBlank
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String merchantId;
    @NotBlank
    private String type;
    @Size(min = 3, max = 3)
    private String currencyCode;
    @NotBlank
    private String credential;
    @NotBlank
    private String credentialReference;
    @Size(min = 8, max = 8)
    private String terminalId;
    @Size(min = 15, max = 15)
    private String cardAcceptorId;
    private String fiId;
    private boolean active;
}
