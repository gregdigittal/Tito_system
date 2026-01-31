package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Accessors(chain = true)
public class OffloadPaymentRequestMoz {

    @NotEmpty(message = "Invalid tag")
    private String tag;

    @NotEmpty(message = "Invalid 'offloadTransactions' parameter")
    private List<String> offloadTransactions;
}