package cash.ice.fbc.dto.flexcube;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class FlexcubeResponse {
    private String resultCode;
    private String resultDescription;
    private String hostReference;
    private String externalReference;
    private String icecashReference;
    private String paymentDetails;
}
