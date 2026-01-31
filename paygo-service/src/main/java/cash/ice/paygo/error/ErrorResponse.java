package cash.ice.paygo.error;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@AllArgsConstructor
@Data
public class ErrorResponse {
    private Instant date;
    private String errorCode;
    private String message;
}
