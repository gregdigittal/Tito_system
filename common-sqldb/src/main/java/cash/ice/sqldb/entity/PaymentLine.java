package cash.ice.sqldb.entity;

import cash.ice.sqldb.converter.JsonToMapConverter;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "payment_line")
@Data
@Accessors(chain = true)
public class PaymentLine implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "payment_id", nullable = false)
    private Integer paymentId;

    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Column(name = "details")
    private String details;

    @Column(name = "transaction_id")
    private Integer transactionId;

    @Column(name = "meta_data", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> meta;
}
