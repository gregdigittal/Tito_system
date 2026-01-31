package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "fee")
@Data
@Accessors(chain = true)
public class Fee implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "charge_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ChargeType chargeType;

    @Column(name = "src_amount_fee_id")
    private Integer srcAmountFeeId;

    @Column(name = "process_order")
    private Integer processOrder;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "min_charge")
    private BigDecimal minCharge;

    @Column(name = "max_charge")
    private BigDecimal maxCharge;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_code_id", nullable = false)
    private TransactionCode transactionCode;

    @Column(name = "currency_id")
    private Integer currencyId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dr_entity_account_id")
    private Account drEntityAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cr_entity_account_id")
    private Account crEntityAccount;

    @Column(name = "affordability_check", nullable = false)
    private boolean affordabilityCheck;

    @Column(name = "active", nullable = false)
    private boolean active;

    public boolean isOriginal() {
        return chargeType == ChargeType.ORIGINAL;
    }
}
