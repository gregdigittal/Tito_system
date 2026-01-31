package cash.ice.fbc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.Instant;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class FbcStatusResponse {
    private Integer statusCode;
    private Response response;

    private Instant timestamp;
    private Integer status;
    private String error;

    @Data
    public static class Response {
        private String beneficiaryOrg;
        private String clientId;
        private String clientName;
        private String creditAccount;
        private String creditBranch;
        private String debitAccount;
        private BigDecimal debitAmount;
        private String debitBranch;
        private String debitCurrency;
        private String debitValueDate;
        private String externalReference;
        private String hostReference;
        private String paymentDetails;
        private String resultCode;
        private String resultDescription;
    }
}
