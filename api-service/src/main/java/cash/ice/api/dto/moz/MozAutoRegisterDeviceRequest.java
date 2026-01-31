package cash.ice.api.dto.moz;

import cash.ice.common.utils.Tool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MozAutoRegisterDeviceRequest {
    @NotEmpty(message = "Invalid serial number")
    private String serialNumber;
    private Map<String, Object> metaData = new HashMap<>();
    private String productNumber;
    private String model;
    private String bootVersion;
    private String cpuType;
    private String rfidVersion;
    private String osVersion;
    private String imei;
    private String imsi;

    @JsonIgnore
    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(String jsonString) {
        this.metaData = Tool.jsonStringToMap(jsonString);
    }
}
