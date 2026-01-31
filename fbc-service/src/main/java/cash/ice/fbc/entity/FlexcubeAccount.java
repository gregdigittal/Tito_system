package cash.ice.fbc.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

@Document(collection = "FlexcubeAccount")
@Data
@Accessors(chain = true)
public class FlexcubeAccount {

    @Id
    private String id;

    private List<String> transactionCodes;
    private String debitPoolAccount;
    private String debitPoolAccountBranch;
    private BigDecimal transactionFee;
    private BigDecimal minBalanceMargin;
    private BigDecimal balanceWarningValue;
}
