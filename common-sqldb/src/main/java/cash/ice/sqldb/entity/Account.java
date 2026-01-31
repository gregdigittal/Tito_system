package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account")
@Data
@Accessors(chain = true)
public class Account implements Serializable {
    public static final String NUMBER_PREFIX = "3";
    public static final int NUMBER_LENGTH = 11;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "account_type_id")
    private Integer accountTypeId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "account_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    @Column(name = "daily_limit")
    private BigDecimal dailyLimit;

    @Column(name = "overdraft_limit")
    private BigDecimal overdraftLimit;

    @Column(name = "balance_minimum")
    private BigDecimal balanceMinimum;

    @Column(name = "balance_warning")
    private BigDecimal balanceWarning;

    @Column(name = "enforce_balance_limits", nullable = false)
    private boolean balanceMinimumEnforce;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(name = "auto_debit", nullable = false)
    private boolean autoDebit;

    @Column(name = "authorisation_type")
    @Enumerated(EnumType.STRING)
    private AuthorisationType authorisationType;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    public String getAuthorisationTypeString() {
        return authorisationType != null ? authorisationType.name() : null;
    }
}
