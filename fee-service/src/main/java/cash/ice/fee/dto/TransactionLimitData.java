package cash.ice.fee.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "TransactionLimitData")
@Data
@Accessors(chain = true)
public class TransactionLimitData {

    @Id
    private String id;
    private Integer transactionLimitId;
    private int day;
    private int dayTransactions;
    private BigDecimal dayAmount;
    private int week;
    private int weekTransactions;
    private BigDecimal weekAmount;
    private int month;
    private int monthTransactions;
    private BigDecimal monthAmount;
    private LocalDateTime lastUpdate;
    @Transient
    private BigDecimal lastAmount;
}
