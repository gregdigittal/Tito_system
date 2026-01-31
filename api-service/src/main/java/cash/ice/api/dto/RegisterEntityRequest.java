package cash.ice.api.dto;

import cash.ice.sqldb.entity.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Locale;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class RegisterEntityRequest {

    private String firstName;
    @NotEmpty(message = "Last name cannot be null")
    private String lastName;
    @NotEmpty(message = "ID Type cannot be null")
    private String idTypeId;
    @NotEmpty(message = "ID Number cannot be null")
    private String idNumber;
    @NotEmpty(message = "Entity Type cannot be null")
    private String entityType;
    @NotEmpty(message = "Mobile number cannot be null")
    private String mobile;
    @Email(message = "Email must be valid")
    private String email;
    private String company;
    private String card;
    private EntityStatus status;
    private LoginStatus loginStatus;
    private KYC kycStatus;
    private LimitTier transactionLimitTier;
    private Boolean corporateFee;
    private Gender gender;
    private AuthorisationType authorisationType;
    private String contactName;
    private String altMobile;
    private String altContactName;
    private Locale locale;
    private Integer citizenshipCountryId;
    private Address address;

    @Data
    public static class Address {
        private Integer countryId;
        private String city;
        private String postalCode;
        private String address1;
        private String address2;
        private String notes;
    }
}
