package cash.ice.fbc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class FbcLoginRequest {
    private String username;
    private String password;
}
