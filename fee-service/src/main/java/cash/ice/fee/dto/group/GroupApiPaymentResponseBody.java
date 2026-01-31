package cash.ice.fee.dto.group;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class GroupApiPaymentResponseBody {

    @SerializedName("Vendor_Ref")
    private String vendorRef;

    @SerializedName("Result")
    private String result;

    @SerializedName("Message")
    private String message;

    @SerializedName("Transaction_ID")
    private String transactionId;

    @SerializedName("Balance")
    private String balance;

    @SerializedName("Warning")
    private String warning;

    @SerializedName("posRRN")
    private String posRrn;

    @SerializedName("posTDT")
    private String posTdt;

    @SerializedName("posTAN")
    private String posTan;
}
