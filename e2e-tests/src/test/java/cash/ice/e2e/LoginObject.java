package cash.ice.e2e;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginObject {
    private String accessToken;
    private String refreshToken;
}
