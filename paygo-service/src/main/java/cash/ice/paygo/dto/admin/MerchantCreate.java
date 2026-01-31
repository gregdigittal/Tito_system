package cash.ice.paygo.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MerchantCreate {
    @NotBlank
    private String transactionCode;
    @NotBlank
    private String name;
    @Size(min = 3, max = 3)
    private String countryCode;
    @NotBlank
    private String city;
    @NotBlank
    private String region;
    @NotBlank
    private String addressLine1;
    @NotNull
    private String addressLine2;
    @NotNull
    private String description;
    @NotNull
    @Email
    private String emailAddress;
    @NotNull
    private String phoneNumber;
    private String url;
    private String mspReference;
    private boolean active;
}
