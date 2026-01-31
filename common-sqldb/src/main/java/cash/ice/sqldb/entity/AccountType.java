package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "account_type")
@Data
@Accessors(chain = true)
public class AccountType implements Serializable {
    public static final String PRIMARY_ACCOUNT = "Primary";
    public static final String SUBSIDY_ACCOUNT = "Subsidy";
    public static final String PREPAID_TRANSPORT = "Prepaid";
    public static final String FNDS_ACCOUNT = "FNDS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "currency_id")
    private Integer currencyId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "audit_transaction_interval")
    private Integer auditTransactionInterval;

    @Column(name = "audit_transaction_value")
    private BigDecimal auditTransactionValue;

    @Column(name = "legacy_wallet_id")
    private String legacyWalletId;

    @Column(name = "active", nullable = false)
    private boolean active;
}
