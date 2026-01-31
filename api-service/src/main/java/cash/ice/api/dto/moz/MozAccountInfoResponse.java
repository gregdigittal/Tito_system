package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MozAccountInfoResponse {
    private Integer accountId;
    private String accountNumber;
    private String accountType;
    private String firstName;
    private String lastName;
    private String deviceCode;
    private String deviceStatus;
    private Integer vehicleId;
}
