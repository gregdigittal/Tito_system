package cash.ice.zim.api.dto;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Map;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class CreateTransactionCardSpResult {
    private String spName;
    private Integer transactionId;
    private Integer drAccountId;
    private Integer crAccountId;
    private BigDecimal balance;
    private BigDecimal drFees;
    private Integer result;
    private String message;
    private String error;

    public CreateTransactionCardSpResult(String spName, Map<String, Object> spResult) {
        this.spName = spName;
        this.transactionId = (Integer) spResult.get("Transaction_ID");
        this.drAccountId = (Integer) spResult.get("DR_Account_ID");
        this.crAccountId = (Integer) spResult.get("CR_Account_ID");
        this.balance = (BigDecimal) spResult.get("Balance");
        this.drFees = (BigDecimal) spResult.get("DR_Fees");
        this.result = (Integer) spResult.get("Result");
        this.message = (String) spResult.get("Message");
        this.error = (String) spResult.get("Error");
    }
}
