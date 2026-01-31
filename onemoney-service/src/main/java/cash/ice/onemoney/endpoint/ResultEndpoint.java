package cash.ice.onemoney.endpoint;

import cash.ice.onemoney.error.OnemoneyException;
import cash.ice.onemoney.service.OnemoneyPaymentService;
import com.huawei.cps.cpsinterface.api_resultmgr.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
@Slf4j
public class ResultEndpoint {
    private static final String NAMESPACE_URI = "http://cps.huawei.com/cpsinterface/api_resultmgr";

    @Autowired
    private OnemoneyPaymentService onemoneyPaymentService;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "Result")
    @ResponsePayload
    public void paymentResult(@RequestPayload Result result) {
        try {
            log.info("> {}", ToStringBuilder.reflectionToString(result, new RecursiveToStringStyle()));
            onemoneyPaymentService.callbackResult(result);

        } catch (Exception e) {
            if (e instanceof OnemoneyException && ((OnemoneyException) e).getOnemoneyPayment() != null) {
                onemoneyPaymentService.failPayment(((OnemoneyException) e).getOnemoneyPayment(),
                        ((OnemoneyException) e).getErrorCode(), e.getMessage(), null);
            } else {
                log.error(e.getMessage(), e);
            }
        }
    }
}
