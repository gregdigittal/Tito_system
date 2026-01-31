package cash.ice.ecocash.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class EcocashCallbackPayment {
    private String clientCorrelator;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String notifyUrl;
    private String referenceCode;
    private String tranType;
    private String endUserId;
    private String transactionOperationStatus;
    private PaymentAmount paymentAmount;
    private String currencyCode;
    private String countryCode;
    private String terminalId;
    private String location;
    private String superMerchantName;
    private String merchantName;
    private String merchantCode;
    private String merchantPin;
    private String merchantNumber;
    private String remarks;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String originalEcocashReference;

    public static EcocashCallbackPayment createPurchase() {
        return new EcocashCallbackPayment().setTranType("MER").setTransactionOperationStatus("Charged");
    }

    public static EcocashCallbackPayment createRefund(EcocashCallbackPayment paymentRequest) {
        return new EcocashCallbackPayment()
                .setTranType("REF")
                .setClientCorrelator(paymentRequest.getClientCorrelator())
                .setReferenceCode(paymentRequest.getReferenceCode())
                .setEndUserId(paymentRequest.getEndUserId())
                .setPaymentAmount(new PaymentAmount()
                        .setChargeMetaData(new ChargeMetaData()
                                .setChannel(paymentRequest.getPaymentAmount().getChargeMetaData().getChannel())
                                .setPurchaseCategoryCode(paymentRequest.getPaymentAmount().getChargeMetaData().getPurchaseCategoryCode())
                                .setOnBeHalfOf(paymentRequest.getPaymentAmount().getChargeMetaData().getOnBeHalfOf()))
                        .setCharginginformation(new ChargingInformation()
                                .setAmount(paymentRequest.getPaymentAmount().getCharginginformation().getAmount())
                                .setCurrency(paymentRequest.getPaymentAmount().getCharginginformation().getCurrency())
                                .setDescription(paymentRequest.getPaymentAmount().getCharginginformation().getDescription())))
                .setMerchantCode(paymentRequest.getMerchantCode())
                .setMerchantPin(paymentRequest.getMerchantPin())
                .setMerchantNumber(paymentRequest.getMerchantNumber())
                .setCurrencyCode(paymentRequest.getCurrencyCode())
                .setCountryCode(paymentRequest.getCountryCode())
                .setTerminalId(paymentRequest.getTerminalId())
                .setLocation(paymentRequest.getLocation())
                .setSuperMerchantName(paymentRequest.getSuperMerchantName())
                .setMerchantName(paymentRequest.getMerchantName());
    }
}
