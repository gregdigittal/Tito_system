package cash.ice.fee.dto.group;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupApiPaymentRequestBody {
    private String appVersion;

    @SerializedName("Vendor_Date")
    private String vendorDate;

    @SerializedName("Vendor_Ref")
    private String vendorRef;

    @SerializedName("Location_ID")
    private String locationId;

    @SerializedName("Origin_ID")
    private String originId;

    @SerializedName("TX")
    private String tx;

    @SerializedName("ThirdParty")
    private String thirdParty;

    @SerializedName("Location_Details")
    private String locationDetails;

    @SerializedName("TX_Details")
    private String txDetails;

    @SerializedName("VRN")
    private String vrn;

    @SerializedName("Identifier_Type")
    private String identifierType;

    @SerializedName("Identifier")
    private String identifier;

    @SerializedName("Identifier_PVV")
    private String identifierPvv;

    @SerializedName("Identifier_CVV")
    private String identifierCvv;

    @SerializedName("Identifier_Expiry")
    private String identifierExpiry;

    @SerializedName("Card_Track2")
    private String cardTrack2;

    @SerializedName("POS_ID")
    private String posID;

    @SerializedName("Status")
    private String status;

    @SerializedName("Driver_ID")
    private String driverId;

    @SerializedName("Agency_ID")
    private String agencyId;

    @SerializedName("User_Detail")
    private String userDetail;

    @SerializedName("ICECash_Wallet")
    private String iceCashWallet;

    @SerializedName("Payment_ThirdParty")
    private String paymentThirdParty;

    @SerializedName("Payment_Card")
    private String paymentCard;

    @SerializedName("Payment_VRN")
    private String paymentVrn;

    @SerializedName("Payment_Agency_ID")
    private String paymentAgencyId;

    @SerializedName("TX_Analysis")
    private GroupApiPaymentTxAnalysis txAnalysis;

    @SerializedName("Agent")
    private GroupApiAgent agent;

}
