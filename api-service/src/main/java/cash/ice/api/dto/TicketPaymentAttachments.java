package cash.ice.api.dto;

import cash.ice.common.dto.PaymentRequest;
import cash.ice.sqldb.entity.TicketStatus;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "TicketPaymentAttachments")
@Data
@Accessors(chain = true)
public class TicketPaymentAttachments {

    @Id
    private String id;
    private Integer ticketId;
    private TicketStatus status;
    private String vendorRef;
    private String description;
    private PaymentRequest paymentRequest;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdDate;
}
