package cash.ice.api.dto;

import cash.ice.api.entity.zim.PaymentCollection;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentCollectionView {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer id;
    private Integer sessionId;
    private Integer paymentMethodId;
    private String channel;
    private LocalDateTime createdDate;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Integer> paymentsIds = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<PaymentView> payments = new ArrayList<>();

    public PaymentCollection toPaymentCollection() {
        return new PaymentCollection()
                .setId(id)
                .setSessionId(sessionId)
                .setPaymentMethodId(paymentMethodId)
                .setChannel(channel)
                .setCreatedDate(createdDate);
    }

    public static PaymentCollectionView create(PaymentCollection paymentCollection) {
        return new PaymentCollectionView()
                .setId(paymentCollection.getId())
                .setSessionId(paymentCollection.getSessionId())
                .setPaymentMethodId(paymentCollection.getPaymentMethodId())
                .setChannel(paymentCollection.getChannel())
                .setCreatedDate(paymentCollection.getCreatedDate());
    }
}
