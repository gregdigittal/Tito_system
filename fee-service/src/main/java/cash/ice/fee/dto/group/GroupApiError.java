package cash.ice.fee.dto.group;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class GroupApiError {

    @SerializedName("Error")
    private GroupApiErrorBody error;
}
