package cash.ice.posb.dto;

import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.posb.dto.posb.*;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;

@MappedEntity("PosbPayment")
@Data
@Accessors(chain = true)
public class PosbPayment {

    @Id
    private String vendorRef;
    private String traceId;
    private String reason;
    private String status;
    private Instant createdTime;
    private Instant updatedTime;
    private Instant refundedTime;
    private PaymentRequestZim paymentRequest;
    private PosbInstructionRequest instructionRequest;
    private PosbInstructionResponse instructionResponse;
    private PosbConfirmationRequest confirmationRequest;
    private PosbConfirmationResponse confirmationResponse;
    private PosbStatusResponse statusResponse;
    private PosbReversalRequest reversalRequest;
    private PosbReversalResponse reversalResponse;
}
