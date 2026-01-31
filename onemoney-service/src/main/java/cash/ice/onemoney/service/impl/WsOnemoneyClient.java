package cash.ice.onemoney.service.impl;

import cash.ice.onemoney.service.OnemoneyClient;
import com.huawei.cps.cpsinterface.api_requestmgr.Request;
import com.huawei.cps.cpsinterface.api_requestmgr.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;

@RequiredArgsConstructor
@Slf4j
public class WsOnemoneyClient extends WebServiceGatewaySupport implements OnemoneyClient {

    @Override
    public Response sendPayment(Request request, String paymentUrl) {
        log.debug("  sending to {}, ResultURL: {}", paymentUrl, request.getHeader().getCaller().getResultURL());
        return (Response) getWebServiceTemplate()
                .marshalSendAndReceive(paymentUrl, request, new SoapActionCallback(paymentUrl));
    }

    @Override
    public com.huawei.cps.synccpsinterface.api_requestmgr.Result sendStatusRequest(com.huawei.cps.synccpsinterface.api_requestmgr.Request request, String statusUrl) {
        log.debug("  polling {}", statusUrl);
        return (com.huawei.cps.synccpsinterface.api_requestmgr.Result) getWebServiceTemplate()
                .marshalSendAndReceive(statusUrl, request, new SoapActionCallback(statusUrl));
    }

    @Override
    public Response sendRefundRequest(Request request, String reversalUrl) {
        log.debug("  refunding {}", reversalUrl);
        return (Response) getWebServiceTemplate()
                .marshalSendAndReceive(reversalUrl, request, new SoapActionCallback(reversalUrl));
    }
}
