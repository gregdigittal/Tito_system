package cash.ice.paygo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class AdditionalData {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DirectoryQuery directoryQuery;
    private ComplianceData complianceData;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DirectoryService directoryService;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String entryType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String pointOfInitiation;
}
