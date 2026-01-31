package cash.ice.onemoney.util;

import cash.ice.common.utils.Tool;
import cash.ice.onemoney.config.OnemoneyProperties;
import com.huawei.cps.cpsinterface.api_requestmgr.Request;
import com.huawei.cps.cpsinterface.request.Body;
import com.huawei.cps.cpsinterface.request.Header;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class RequestBuilder {
    private final OnemoneyProperties onemoneyProperties;
    private final Request request;

    public RequestBuilder(OnemoneyProperties onemoneyProperties) {
        this.onemoneyProperties = onemoneyProperties;
        request = new Request();

        Body body = new Body();
        Body.Identity identity = new Body.Identity();
        body.setIdentity(identity);
        request.setBody(body);
    }

    public RequestBuilder addHeader(String commandID) {
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

    public RequestBuilder addInitiator(String identifier) {
        Body.Identity.Initiator initiator = new Body.Identity.Initiator();
        initiator.setIdentifierType(onemoneyProperties.getRequest().getInitiatorIdentifierType());
        initiator.setIdentifier(identifier);
        request.getBody().getIdentity().setInitiator(initiator);
        return this;
    }

    public RequestBuilder addOnemoneyInitiator() {
        Body.Identity.Initiator initiator = new Body.Identity.Initiator();
        initiator.setIdentifierType(onemoneyProperties.getRequest().getOnemoneyInitiatorIdentifierType());
        initiator.setIdentifier(onemoneyProperties.getRequest().getOnemoneyInitiatorIdentifier());
        initiator.setSecurityCredential(onemoneyProperties.getRequest().getOnemoneySecurityCredential());
        request.getBody().getIdentity().setInitiator(initiator);
        return this;
    }

    public RequestBuilder addIdentityReceiverParty() {
        Body.Identity.ReceiverParty receiverParty = new Body.Identity.ReceiverParty();
        receiverParty.setIdentifierType(onemoneyProperties.getRequest().getReceiverIdentifierType());
        receiverParty.setIdentifier(onemoneyProperties.getRequest().getReceiverIdentifier());
        request.getBody().getIdentity().setReceiverParty(receiverParty);
        return this;
    }

    public RequestBuilder addTransactionRequest(BigDecimal amount, String currencyCode) {
        Body.TransactionRequest transactionRequest = new Body.TransactionRequest();
        Body.TransactionRequest.Parameters parameters = new Body.TransactionRequest.Parameters();
        parameters.setAmount(amount);
        parameters.setCurrency(currencyCode);
        transactionRequest.setParameters(parameters);
        request.getBody().setTransactionRequest(transactionRequest);
        return this;
    }

    public RequestBuilder addRaiseDisputedTxnReversalRequest(String originalReceiptNumber, BigDecimal originalAmount) {
        Body.RaiseDisputedTxnReversalRequest raiseDisputedTxnReversalRequest = new Body.RaiseDisputedTxnReversalRequest();
        raiseDisputedTxnReversalRequest.setOriginalReceiptNumber(originalReceiptNumber);
        raiseDisputedTxnReversalRequest.setAmount(originalAmount);
        request.getBody().setRaiseDisputedTxnReversalRequest(raiseDisputedTxnReversalRequest);
        return this;
    }

    public RequestBuilder addRemark(String remark) {
        request.getBody().setRemark(remark);
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
