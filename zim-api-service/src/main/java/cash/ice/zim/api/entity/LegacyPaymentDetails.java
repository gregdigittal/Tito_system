package cash.ice.zim.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "Payment_Details")
@Data
@Accessors(chain = true)
public class LegacyPaymentDetails implements Serializable {

    @Id
    @Column(name = "Payment_Detail_ID")
    private Integer paymentDetailId;

    @Column(name = "Payment_ID")
    private Integer paymentId;

    @Column(name = "Account_ID")
    private Integer accountId;

    @Column(name = "Amount")
    private BigDecimal amount;
}
