package cash.ice.onemoney.util;

import cash.ice.common.utils.Tool;
import cash.ice.onemoney.config.OnemoneyProperties;
import com.huawei.cps.synccpsinterface.api_requestmgr.Request;
import com.huawei.cps.synccpsinterface.request.Header;
import com.huawei.cps.synccpsinterface.request.Body;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class StatusRequestBuilder {
    private final OnemoneyProperties onemoneyProperties;
    private final Request request;

    public StatusRequestBuilder(OnemoneyProperties onemoneyProperties) {
        this.onemoneyProperties = onemoneyProperties;
        request = new Request();

        Body body = new Body();
        Body.Identity identity = new Body.Identity();
        body.setIdentity(identity);
        request.setBody(body);
    }

    public StatusRequestBuilder addHeader(String commandID) {
        Header header = new Header();
        header.setVersion(onemoneyProperties.getRequest().getHeaderVersion());
        header.setCommandID(commandID);
        header.setOriginatorConversationID(createConversationId());
        Header.Caller caller = new Header.Caller();
        caller.setCallerType(onemoneyProperties.getRequest().getCallerType());
        caller.setThirdPartyID(onemoneyProperties.getRequest().getThirdPartyId());
        caller.setPassword(onemoneyProperties.getRequest().getPassword());
        caller.setResultURL(onemoneyProperties.getResult().getUrlPrefix() +
                onemoneyProperties.getResult().getLocationUri());
        header.setCaller(caller);
        header.setKeyOwner(onemoneyProperties.getRequest().getKeyOwner());
        header.setTimestamp(getCurrentTimestamp());
        request.setHeader(header);
        return this;
    }

    public StatusRequestBuilder addOnemoneyInitiator() {
        Body.Identity.Initiator initiator = new Body.Identity.Initiator();
        initiator.setIdentifierType(onemoneyProperties.getRequest().getOnemoneyInitiatorIdentifierType());
        initiator.setIdentifier(onemoneyProperties.getRequest().getOnemoneyInitiatorIdentifier());
        initiator.setSecurityCredential(onemoneyProperties.getRequest().getOnemoneySecurityCredential());
        request.getBody().getIdentity().setInitiator(initiator);
        return this;
    }

    public StatusRequestBuilder addQueryTransactionStatusRequest(String originalConversationId) {
        Body.QueryTransactionStatusRequest queryTransactionStatusRequest = new Body.QueryTransactionStatusRequest();
        queryTransactionStatusRequest.setOriginalConversationID(originalConversationId);
        request.getBody().setQueryTransactionStatusRequest(queryTransactionStatusRequest);
        return this;
    }

    public StatusRequestBuilder addRemark() {
        request.getBody().setRemark(onemoneyProperties.getRequest().getDefaultRemark());
        return this;
    }

    public Request build() {
        return request;
    }

    private String getCurrentTimestamp() {
        return DateTimeFormatter.ofPattern(onemoneyProperties.getRequest().getTimestampPattern())
                .format(Instant.now().atZone(Tool.getZimZoneId()));
    }

    private String createConversationId() {
        String uuid = UUID.randomUUID().toString();
        return uuid.substring(0, 8) + uuid.substring(9, 13) + uuid.substring(14, 18) + uuid.substring(19, 23);
    }
}
