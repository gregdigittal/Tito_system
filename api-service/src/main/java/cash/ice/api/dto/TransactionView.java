package cash.ice.api.dto;

import cash.ice.sqldb.entity.Transaction;
import cash.ice.sqldb.entity.TransactionLines;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TransactionView {
    private Integer id;
    private String sessionId;
    private Integer transactionCodeId;
    private Integer currencyId;
    private BigDecimal amount;
    private Integer initiatorId;
    private Integer initiatorTypeId;
    private Integer channelId;
    private LocalDateTime createdDate;
    private LocalDateTime statementDate;
    private List<TransactionLines> lines = new ArrayList<>();

    public static TransactionView create(Transaction transaction, List<TransactionLines> lines) {
        return new TransactionView()
                .setId(transaction.getId())
                .setSessionId(transaction.getSessionId())
                .setTransactionCodeId(transaction.getTransactionCodeId())
                .setCurrencyId(transaction.getCurrencyId())
                .setInitiatorId(transaction.getInitiatorId())
                .setInitiatorTypeId(transaction.getInitiatorTypeId())
                .setChannelId(transaction.getChannelId())
                .setCreatedDate(transaction.getCreatedDate())
                .setStatementDate(transaction.getStatementDate())
                .setLines(lines)
                .setAmount(lines.stream().map(TransactionLines::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
