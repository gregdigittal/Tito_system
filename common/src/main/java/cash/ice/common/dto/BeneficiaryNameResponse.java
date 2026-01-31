package cash.ice.common.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BeneficiaryNameResponse {
    private String msisdn;
    private String name;
    private String errorCode;
    private String errorMessage;
}
