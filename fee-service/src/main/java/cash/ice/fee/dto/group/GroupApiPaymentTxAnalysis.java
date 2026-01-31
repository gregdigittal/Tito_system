package cash.ice.fee.dto.group;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupApiPaymentTxAnalysis {

    @SerializedName("ZLF")
    private String zlf;

    @SerializedName("ZRF")
    private String zrf;
}
