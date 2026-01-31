package cash.ice.paygo.entity;

import cash.ice.paygo.dto.PaygoCallbackRequest;
import cash.ice.paygo.dto.PaygoCallbackResponse;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "PaygoCallbackLog")
@Data
@Accessors(chain = true)
public class PaygoCallbackLog {
    @Id
    private String id;

    private LocalDateTime time;
    private String vendorRef;
    private String payGoId;
    private String paymentReference;
    private String deviceReference;
    private String messageType;
    private String responseCodeReceived;
    private String responseCodeSent;
    private PaygoCallbackRequest request;
    private PaygoCallbackResponse response;
    private Object errorMessage;
    private Object errorStackTrace;
}
