package cash.ice.emola.service.impl;

import cash.ice.emola.service.EmolaClient;
import com.viettel.bccsgw.webservice.GwOperation;
import com.viettel.bccsgw.webservice.GwOperationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

@RequiredArgsConstructor
@Slf4j
public class WsEmolaClient extends WebServiceGatewaySupport implements EmolaClient {

    @Override
    public GwOperationResponse sendPayment(GwOperation request, String paymentUrl) {
        log.debug("  sending {}", paymentUrl);
        return (GwOperationResponse) getWebServiceTemplate().marshalSendAndReceive(paymentUrl, request);
//        return (GwOperationResponse) getWebServiceTemplate().marshalSendAndReceive(paymentUrl, request, new SoapActionCallback(paymentUrl));
    }
}
