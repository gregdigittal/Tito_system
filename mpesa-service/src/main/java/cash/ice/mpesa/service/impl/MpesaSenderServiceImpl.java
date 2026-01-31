package cash.ice.mpesa.service.impl;

import cash.ice.common.utils.Tool;
import cash.ice.mpesa.config.MpesaProperties;
import cash.ice.mpesa.service.MpesaSenderService;
import com.fc.sdk.APIContext;
import com.fc.sdk.APIRequest;
import com.fc.sdk.APIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpesaSenderServiceImpl implements MpesaSenderService {
    private final MpesaProperties mpesaProperties;

    @Override
    public APIResponse sendInboundRequest(String transactionReference, String msisdn, BigDecimal amount) {
        return sendRequest(transactionReference, msisdn, amount, mpesaProperties.getC2bRequest(), "C2B");
    }

    @Override
    public APIResponse sendOutboundRequest(String transactionReference, String msisdn, BigDecimal amount) {
        return sendRequest(transactionReference, msisdn, amount, mpesaProperties.getB2cRequest(), "B2C");
    }

    private APIResponse sendRequest(String transactionReference, String msisdn, BigDecimal amount, MpesaProperties.Request requestProps, String reqType) {
        APIContext context = createApiContext(requestProps);
        context.addParameter("input_TransactionReference", transactionReference);
        context.addParameter("input_ThirdPartyReference", transactionReference);
        context.addParameter("input_CustomerMSISDN", msisdn);
        context.addParameter("input_ServiceProviderCode", mpesaProperties.getIcecashBusinessShortcode());
        context.addParameter("input_Amount", amount.toString());
        log.debug("  sending {}, ref: {}, msisdn: {}, businessCode: {}, amount: {}", reqType, transactionReference, msisdn, mpesaProperties.getIcecashBusinessShortcode(), amount);
        APIResponse response = new APIRequest(context).execute();
        if (response != null) {
            log.debug("  response: statusCode={}, reason='{}', statusLine='{}', result={}, params: {}", response.getStatusCode(), response.getReason(), response.getStatusLine(), response.getResult(), response.getParameters());
        } else {
            log.debug("  response: null");
        }
        return response;
    }

    @Override
    public APIResponse sendB2bRequest(String transactionReference, String primaryBusinessCode, String receiverBusinessCode, BigDecimal amount) {
        APIContext context = createApiContext(mpesaProperties.getB2bRequest());
        context.addParameter("input_TransactionReference", transactionReference);
        context.addParameter("input_ThirdPartyReference", transactionReference);
        context.addParameter("input_PrimaryPartyCode", primaryBusinessCode);
        context.addParameter("input_ReceiverPartyCode", receiverBusinessCode);
        context.addParameter("input_Amount", amount.toString());
        log.debug("  sending B2B, ref: {}, primaryBusinessCode: {}, receiverBusinessCode: {}, amount: {}",
                transactionReference, primaryBusinessCode, receiverBusinessCode, amount);
        APIResponse response = new APIRequest(context).execute();
        if (response != null) {
            log.debug("  response: statusCode={}, reason='{}', statusLine='{}', result={}, params: {}", response.getStatusCode(), response.getReason(), response.getStatusLine(), response.getResult(), response.getParameters());
        } else {
            log.debug("   response: null");
        }
        return response;
    }

    @Override
    public APIResponse sendReversalRequest(String transactionId, BigDecimal optionalAmount) {
        String requestReference = Tool.generateCharacters(16);
        APIContext context = createApiContext(mpesaProperties.getReversalRequest());
        context.addParameter("input_TransactionID", transactionId);
        context.addParameter("input_ThirdPartyReference", requestReference);
        context.addParameter("input_SecurityCredential", mpesaProperties.getSecurityCredential());
        context.addParameter("input_InitiatorIdentifier", mpesaProperties.getInitiatorIdentifier());
        context.addParameter("input_ServiceProviderCode", mpesaProperties.getIcecashBusinessShortcode());
        if (optionalAmount != null) {
            context.addParameter("input_ReversalAmount", mpesaProperties.isFillOptionalReversalAmount() ? optionalAmount.toString() : null);
        }
        log.debug("  sending reversal, transactionId: {}, securityCredential: {}, initiatorIdentifier: {}, businessCode: {}, optionalAmount: {}, thirdPartyRef: {}",
                transactionId, mpesaProperties.getSecurityCredential(), mpesaProperties.getInitiatorIdentifier(), mpesaProperties.getIcecashBusinessShortcode(), optionalAmount, requestReference);
        APIResponse response = new APIRequest(context).execute();
        if (response != null) {
            log.debug("  response: statusCode={}, reason='{}', statusLine='{}', result={}, params: {}", response.getStatusCode(), response.getReason(), response.getStatusLine(), response.getResult(), response.getParameters());
        } else {
            log.debug("  response: null ");
        }
        return response;
    }

    @Override
    public APIResponse sendQueryTransactionStatusRequest(String transactionOrConversationOrVendorId) {
        APIContext context = createApiContext(mpesaProperties.getQueryTransactionStatusRequest());
        String requestReference = Tool.generateCharacters(16);
        context.addParameter("input_QueryReference", transactionOrConversationOrVendorId);
        context.addParameter("input_ServiceProviderCode", mpesaProperties.getIcecashBusinessShortcode());
        context.addParameter("input_ThirdPartyReference", requestReference);
        log.debug("  sending query transaction status, QueryReference: {}, businessCode: {}, ThirdPartyReference: {}", transactionOrConversationOrVendorId, mpesaProperties.getIcecashBusinessShortcode(), requestReference);
        APIResponse response = new APIRequest(context).execute();
        if (response != null) {
            log.debug("  response: statusCode={}, reason='{}', statusLine='{}', result={}, params: {}", response.getStatusCode(), response.getReason(), response.getStatusLine(), response.getResult(), response.getParameters());
        } else {
            log.debug("  response:  null");
        }
        return response;
    }

    @Override
    public APIResponse sendQueryNameRequest(String msisdn) {
        APIContext context = createApiContext(mpesaProperties.getQueryNameRequest());
        String requestReference = Tool.generateCharacters(16);
        context.addParameter("input_CustomerMSISDN", msisdn);
        context.addParameter("input_ServiceProviderCode", mpesaProperties.getIcecashBusinessShortcode());
        context.addParameter("input_ThirdPartyReference", requestReference);
        log.debug("  sending query name, msisdn: {}, businessCode: {}, thirdPartyRef: {}", msisdn, mpesaProperties.getIcecashBusinessShortcode(), requestReference);
        APIResponse response = new APIRequest(context).execute();
        if (response != null) {
            log.debug("  response: statusCode={}, reason='{}', statusLine='{}', result={}, params: {}", response.getStatusCode(), response.getReason(), response.getStatusLine(), response.getResult(), response.getParameters());
        } else {
            log.debug("   response:  null");
        }
        return response;
    }

    private APIContext createApiContext(MpesaProperties.Request requestProps) {
        APIContext context = new APIContext();
        context.setApiKey(mpesaProperties.getApiKey());
        context.setPublicKey(mpesaProperties.getPublicKey());
        if (mpesaProperties.getOriginHeader() != null && !mpesaProperties.getOriginHeader().isBlank()) {
            context.addHeader("Origin", mpesaProperties.getOriginHeader());
        }
        context.setAddress(requestProps.getAddressHost());
        context.setPort(requestProps.getAddressPort());
        context.setSsl(requestProps.isUseSsl());
        context.setMethodType(requestProps.getMethodType());
        context.setPath(requestProps.getPath());
        log.debug("  request: {} {}:{}{}, ssl: {}, Origin: {}, apiKey: {}, pubKey: {}...{}", requestProps.getMethodType(), requestProps.getAddressHost(), requestProps.getAddressPort(), requestProps.getPath(), requestProps.isUseSsl(), mpesaProperties.getOriginHeader(),
                mpesaProperties.getApiKey(), mpesaProperties.getPublicKey().substring(0, 10), mpesaProperties.getPublicKey().substring(mpesaProperties.getPublicKey().length() - 10));
        return context;
    }
}
