package cash.ice.paygo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaygoCallbackRequest {
    private String acquiringInstitutionId;
    private AdditionalData additionalData;
    private int amount;
    private Card card;
    private String cardAcceptorId;
    private String channel;
    private String created;
    private CreditAccount creditAccount;
    private String currencyCode;
    private String debitReference;
    private String deviceReference;
    private String extendedType;
    private String forwardingInstitutionId;
    private String messageDirection;
    private String messageType;
    private String msisdn;
    private String narration;
    private String network;
    private String originalCreated;
    private String originalParticipantReference;
    private String originalReference;
    private String participantReference;
    private String payee;
    private String paymentMethod;
    private String paymentReference;
    private String reference;
    private String responseCode;
    private String responseDescription;
    private String terminalId;
    private String traceId;
    private String type;
}
