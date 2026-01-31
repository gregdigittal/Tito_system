package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Locale;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class RegisterEntityMozRequest {
    private AccountTypeMoz accountType;
    private String firstName;
    private String lastName;
    private IdTypeMoz idType = IdTypeMoz.ID;
    private String idNumber;
    private Integer idUploadDocumentId;
    private String nuit;
    private Integer nuitUploadDocumentId;
    private String email;
    private String mobile;
    private String pin;
    private Locale locale = Locale.ENGLISH;
    private byte[] photo;
    private String photoFileName;
}
