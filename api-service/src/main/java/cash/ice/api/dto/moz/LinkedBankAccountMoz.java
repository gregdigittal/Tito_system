package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Phase 8-3: Linked bank account view for GraphQL type LinkedBankAccountMoz. */
@Data
@Accessors(chain = true)
public class LinkedBankAccountMoz {
    private Integer id;
    private String bankId;
    private String branchCode;
    private String accountNumber;
    private String accountName;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}
