package cash.ice.fbc.entity;

import cash.ice.common.dto.fee.FeesData;
import cash.ice.fbc.dto.flexcube.FlexcubePaymentRequest;
import cash.ice.fbc.dto.flexcube.FlexcubeResponse;
import cash.ice.fbc.dto.flexcube.FlexcubeStatusRequest;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "FlexcubePayment")
@Data
@Accessors(chain = true)
public class FlexcubePayment {

    @Id
    private String id;

    private String vendorRef;
    private Integer referenceId;
    private String hostReference;
    private String responseResult;
    private String reason;
    private String errorCode;
    private boolean finishedPayment;
    private Instant createdTime;
    private Instant updatedTime;
    private FeesData pendingPayment;
    private List<String> kafkaHeaders;
    private FlexcubePaymentRequest request;
    private FlexcubeStatusRequest statusRequest;
    private FlexcubeResponse response;
}
