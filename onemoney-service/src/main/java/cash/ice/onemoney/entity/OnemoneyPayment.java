package cash.ice.onemoney.entity;

import cash.ice.common.dto.fee.FeesData;
import com.huawei.cps.cpsinterface.api_requestmgr.Request;
import com.huawei.cps.cpsinterface.api_requestmgr.Response;
import com.huawei.cps.cpsinterface.api_resultmgr.Result;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "OnemoneyPayment")
@Data
@Accessors(chain = true)
public class OnemoneyPayment {

    @Id
    private String id;

    private String vendorRef;
    private String originatorConversationId;
    private String transactionId;
    private String resultMessage;
    private String errorMessage;
    private String errorCode;
    private Boolean needCheckStatus;
    private Boolean needRecheckStatus;
    private boolean finishedPayment;
    private Boolean recheckedSuccess;
    private Instant createdTime;
    private Instant updatedTime;
    private FeesData pendingPayment;
    private Request request;
    private Response response;
    private Result result;
    private com.huawei.cps.synccpsinterface.api_requestmgr.Request statusRequest;
    private com.huawei.cps.synccpsinterface.api_requestmgr.Result statusResult;
    private com.huawei.cps.synccpsinterface.api_requestmgr.Result recheckResult;
    private String refundOriginatorConversationId;
    private Request refundRequest;
    private Response refundResponse;
    private Result refundResult;
    private Boolean refundFailed;
    private List<String> kafkaHeaders;
}
