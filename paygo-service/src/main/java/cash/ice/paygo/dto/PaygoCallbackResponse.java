package cash.ice.paygo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaygoCallbackResponse {
    private String acquiringInstitutionId;
    private AdditionalData additionalData;
    private int amount;
    private String cardAcceptorId;
    private String channel;
    private String created;
    private String currencyCode;
    private String extendedType;
    private String forwardingInstitutionId;
    private String messageType;
    private String msisdn;
    private String narration;
    private String network;
    private String participantReference;
    private String payee;
    private String paymentMethod;
    private String reference;
    private String terminalId;
    private String type;
    private String responseCode;
    private String responseDescription;
    private String paymentReference;
    private String deviceReference;
    @JsonIgnore
    private String vendorRef;

    public static PaygoCallbackResponse create(PaygoCallbackRequest request) {
        return new PaygoCallbackResponse()
                .setAcquiringInstitutionId(request.getAcquiringInstitutionId())
                .setAdditionalData(createAdditionalData(request.getAdditionalData()))
                .setAmount(request.getAmount())
                .setCardAcceptorId(request.getCardAcceptorId())
                .setChannel(request.getChannel())
                .setCreated(request.getCreated())
                .setCurrencyCode(request.getCurrencyCode())
                .setExtendedType(request.getExtendedType())
                .setDeviceReference(request.getDeviceReference())
                .setForwardingInstitutionId(request.getForwardingInstitutionId())
                .setMessageType(request.getMessageType())
                .setMsisdn(request.getMsisdn())
                .setNarration(request.getNarration())
                .setNetwork(request.getNetwork())
                .setParticipantReference(request.getParticipantReference())
                .setPayee(request.getPayee())
                .setPaymentMethod(request.getPaymentMethod())
                .setPaymentReference(request.getPaymentReference())
                .setReference(request.getReference())
                .setResponseCode(request.getResponseCode())
                .setResponseDescription(request.getResponseDescription())
                .setTerminalId(request.getTerminalId())
                .setType(request.getType());
    }

    private static AdditionalData createAdditionalData(AdditionalData requestAdditionalData) {
        AdditionalData additionalData = new AdditionalData();
        if (requestAdditionalData != null) {
            additionalData.setComplianceData(requestAdditionalData.getComplianceData());
        }
        return additionalData;
    }
}
