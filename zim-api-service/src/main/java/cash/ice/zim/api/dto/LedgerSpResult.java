package cash.ice.zim.api.dto;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class LedgerSpResult {
    private String spName;
    private Integer result;
    private String message;
    private String error;
    private Integer transactionId;

    public LedgerSpResult(String spName, Map<String, Object> spResult) {
        this.spName = spName;
        this.result = (Integer) spResult.get("Result");
        this.message = (String) spResult.get("Message");
        this.error = (String) spResult.get("Error");
        this.transactionId = (Integer) spResult.get("Transaction_ID");
    }
}
