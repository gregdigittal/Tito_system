package cash.ice.paygo.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class FinancialInstitution {
    private String id;
    private String institutionId;
    private String name;
    private boolean active;
    private String created;
    private String updated;
}
