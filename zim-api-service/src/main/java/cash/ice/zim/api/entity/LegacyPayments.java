package cash.ice.zim.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Entity
@Table(name = "Payments")
@Data
@Accessors(chain = true)
public class LegacyPayments implements Serializable {

    @Id
    @Column(name = "Payment_ID")
    private Integer paymentId;

    @Column(name = "Payment_Collection_ID")
    private Integer paymentCollectionId;

    @Column(name = "Account_ID")
    private Integer accountId;

    @Column(name = "Wallet_ID")
    private Integer walletId;

    @Column(name = "Description")
    private String description;

    @Column(name = "Status")
    private Integer status;

    @Column(name = "Created_By_ID")
    private Integer createdById;
}
