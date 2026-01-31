package cash.ice.ecocash.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class ChargeMetaData {
    private String channel;
    private String purchaseCategoryCode;
    private String onBeHalfOf;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String serviceId;
}
