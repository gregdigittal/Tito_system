package cash.ice.sync.errors;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@AllArgsConstructor
@Data
public class ErrorResponse {
    private Instant date;
    private String errorMessage;
}
