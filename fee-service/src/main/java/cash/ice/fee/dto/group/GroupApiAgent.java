package cash.ice.fee.dto.group;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupApiAgent {

    @SerializedName("Agent_User_ID")
    private String agentUserId;

    @SerializedName("Agent_Name")
    private String agentName;
}
