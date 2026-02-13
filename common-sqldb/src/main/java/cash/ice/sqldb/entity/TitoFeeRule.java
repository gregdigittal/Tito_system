package cash.ice.sqldb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phase 8-10: TiTo fee / device-rental rules. Per country, source account type; percent or fixed amount to TiTo revenue.
 */
@Entity
@Table(name = "tito_fee_rule")
@Data
@Accessors(chain = true)
public class TitoFeeRule implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "country_code", nullable = false, length = 8)
    private String countryCode;

    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType;

    @Column(name = "source_account_type_id")
    private Integer sourceAccountTypeId;

    @Column(name = "share_percent", precision = 5, scale = 2)
    private BigDecimal sharePercent;

    @Column(name = "fixed_amount", precision = 19, scale = 2)
    private BigDecimal fixedAmount;

    @Column(name = "currency_id")
    private Integer currencyId;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
