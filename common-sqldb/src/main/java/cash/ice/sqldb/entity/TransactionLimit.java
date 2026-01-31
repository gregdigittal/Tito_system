package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_limit")
@Data
@Accessors(chain = true)
public class TransactionLimit implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "currency_id", nullable = false)
    private Integer currencyId;

    @Column(name = "transaction_code_id")
    private Integer transactionCodeId;

    @Column(name = "kyc_status_id")
    private Integer kycStatusId;

    @Column(name = "entity_type_id")
    private Integer entityTypeId;

    @Column(name = "account_type_id")
    private Integer accountTypeId;

    @Column(name = "initiator_type_id")
    private Integer initiatorTypeId;

    @Column(name = "tier")
    @Enumerated(EnumType.STRING)
    private LimitTier tier;

    @Column(name = "authorisation_type")
    @Enumerated(EnumType.STRING)
    private AuthorisationType authorisationType;

    @Column(name = "direction")
    @Enumerated(EnumType.STRING)
    private PaymentDirection direction;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "min_limit")
    private BigDecimal transactionMinLimit;

    @Column(name = "max_limit")
    private BigDecimal transactionMaxLimit;

    @Column(name = "daily_limit")
    private BigDecimal dailyLimit;

    @Column(name = "weekly_limit")
    private BigDecimal weeklyLimit;

    @Column(name = "monthly_limit")
    private BigDecimal monthlyLimit;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
