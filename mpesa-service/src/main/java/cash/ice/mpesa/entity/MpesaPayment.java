package cash.ice.mpesa.entity;

import cash.ice.mpesa.dto.Payment;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "MpesaPayment")
@Data
@Accessors(chain = true)
public class MpesaPayment {

    @Id
    private String vendorRef;
    private String status = "init";
    private Payment payment;
    private String responseStatus;
    private String responseCode;
    private String responseDesc;
    private String transactionId;
    private String conversationId;
    private String transactionStatus;
    private String transactionStatusLine;
    private String transactionStatusResponse;
    private String errorCode;
    private String errorMessage;
    private boolean refunded;
    private String refundResponseStatus;
    private String refundResponse;
    private String refundTransactionId;
    private String refundConversationId;
    private String refundTransactionStatus;
    private String refundTransactionStatusResponse;
    private Instant createdTime;
    private Instant updatedTime;
    private Instant refundTime;

    public boolean isPaymentSuccessful() {
        return "INS-0".equals(responseCode);
    }
}
