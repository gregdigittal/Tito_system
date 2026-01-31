package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "transaction_code")
@Data
@Accessors(chain = true)
public class TransactionCode implements Serializable {
    public static final String TSF = "TSF";
    public static final String KIT = "KIT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "kyc_required", nullable = false)
    private boolean kycRequired;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "charge_account_type")
    @Enumerated(EnumType.STRING)
    private ChargeAccountType chargeAccountType;
}
