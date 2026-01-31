package cash.ice.paygo.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class Expiration {
    private String created;
    private String responseCode;
    private String responseDescription;
}
