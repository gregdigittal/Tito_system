package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction_lines")
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TransactionLines {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "transaction_id", nullable = false)
    private Integer transactionId;

    @Column(name = "transaction_code_id", nullable = false)
    private Integer transactionCodeId;

    @Column(name = "entity_account_id", nullable = false)
    private Integer entityAccountId;

    @Column(name = "description")
    private String description;

    @Column(name = "amount")
    private BigDecimal amount;

    public TransactionLines(Integer transactionId, Integer transactionCodeId, Integer entityAccountId, String description, BigDecimal amount) {
        this.transactionId = transactionId;
        this.transactionCodeId = transactionCodeId;
        this.entityAccountId = entityAccountId;
        this.description = description;
        this.amount = amount;
    }
}
