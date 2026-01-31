package cash.ice.common.dto;

import cash.ice.common.dto.fee.FeesData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ErrorData {
    private String errorCode;
    private String message;
    private boolean internalError;
    private FeesData feesData;

    public ErrorData(FeesData feesData, String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.feesData = feesData;
    }
}
