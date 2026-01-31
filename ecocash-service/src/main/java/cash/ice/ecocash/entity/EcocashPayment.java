package cash.ice.ecocash.entity;

import cash.ice.ecocash.dto.EcocashCallbackPayment;
import cash.ice.ecocash.dto.EcocashCallbackPaymentResponse;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "EcocashPayment")
@Data
@Accessors(chain = true)
public class EcocashPayment {

    @Id
    private String vendorRef;

    private String status;
    private String ecocashReference;
    private String endUserId;
    private String clientCorrelator;
    private String transactionOperationStatus;
    private String reason;
    private String errorCode;
    private boolean finishedPayment;
    private Boolean recheck;
    private Boolean recheckedSuccess;
    private Boolean refundFailed;
    private Instant createdTime;
    private Instant updatedTime;
    private Instant refundedTime;
    private Object pendingPayment;
    private EcocashCallbackPayment request;
    private EcocashCallbackPaymentResponse initialResponse;
    private EcocashCallbackPaymentResponse finalResponse;
    private Boolean callbackResponse;
    private EcocashCallbackPayment refundRequest;
    private EcocashCallbackPaymentResponse refundResponse;
    private List<String> kafkaHeaders;
}
