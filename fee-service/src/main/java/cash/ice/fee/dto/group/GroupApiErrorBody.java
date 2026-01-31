package cash.ice.fee.dto.group;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class GroupApiErrorBody {

    @SerializedName("Vendor_Ref")
    private String vendorRef;

    @SerializedName("Result")
    private String result;

    @SerializedName("Error")
    private String error;

    @SerializedName("posRRN")
    private String posRrn;

    @SerializedName("posTDT")
    private String posTdt;

    @SerializedName("posTAN")
    private String posTan;
}
