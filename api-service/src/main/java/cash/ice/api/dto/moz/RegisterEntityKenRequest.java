package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Locale;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class RegisterEntityKenRequest {
    private AccountTypeKen accountType;
    private String firstName;
    private String lastName;
    private IdTypeKen idType = IdTypeKen.NationalID;
    private String idNumber;
    private Integer idUploadDocumentId;
    private Integer biometricUploadDocumentId;
    private String mobile;
    private String email;
    private String pin;
    private Locale locale = Locale.ENGLISH;
}
