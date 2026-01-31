package cash.ice.api.dto.moz;

import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.sqldb.entity.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class OptionalEntityRegisterData {
    private EntityStatus status;
    private LoginStatus loginStatus;
    private Gender gender;
    private Integer citizenshipCountryId;
    private String contactName;
    private String altContactName;
    private String altMobile;
    private AuthorisationType authorisationType;
    private Boolean corporateFee;
    private LimitTier transactionLimitTier;
    private KYC kycStatus;
    private RegisterEntityRequest.Address address;
    private String company;
}
