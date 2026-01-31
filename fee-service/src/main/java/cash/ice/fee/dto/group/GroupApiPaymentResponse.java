package cash.ice.fee.dto.group;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupApiPaymentResponse {

    @SerializedName("Payment_Response")
    private GroupApiPaymentResponseBody paymentResponse;
}
