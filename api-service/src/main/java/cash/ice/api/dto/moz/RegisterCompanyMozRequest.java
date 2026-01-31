package cash.ice.api.dto.moz;

import cash.ice.api.dto.RegisterEntityRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class RegisterCompanyMozRequest {
    private String name;
    private String nuel;
    private Integer nuelUploadDocumentId;
    private String nuit;
    private Integer nuitUploadDocumentId;
    private String mobile;
    private String email;
    private RegisterEntityRequest.Address address;
}
