package cash.ice.api.entity.zim;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_collection")
@Data
@Accessors(chain = true)
public class PaymentCollection implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "session_id", nullable = false)
    private Integer sessionId;

    @Column(name = "payment_method_id")
    private Integer paymentMethodId;

    @Column(name = "channel")
    private String channel;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}
