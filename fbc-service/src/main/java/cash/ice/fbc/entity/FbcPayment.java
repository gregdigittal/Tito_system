package cash.ice.fbc.entity;

import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.fbc.dto.*;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "FbcZimPayment")
@Data
@Accessors(chain = true)
public class FbcPayment {

    @Id
    private String id;

    private String vendorRef;
    private String reason;
    private String status;
    private Instant createdTime;
    private Instant updatedTime;
    private PaymentRequestZim paymentRequest;
    private FbcVerificationResponse verificationResponse;
    private FbcTransferSubmissionRequest transferSubmissionRequest;
    private FbcTransferSubmissionResponse transferSubmissionResponse;
    private FbcVerifyOtpRequest verifyOtpRequest;
    private FbcVerifyOtpResponse verifyOtpResponse;
    private FbcStatusResponse statusResponse;
}
