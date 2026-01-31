package cash.ice.fbc.dto.flexcube;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class FlexcubeBalanceRequest implements Serializable {
    private String account;
    private String branch;
    private String user;
    private String password;
}
