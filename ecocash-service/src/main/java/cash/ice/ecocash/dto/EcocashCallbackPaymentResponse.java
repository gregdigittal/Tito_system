package cash.ice.ecocash.dto;

import cash.ice.common.utils.Tool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Instant;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class EcocashCallbackPaymentResponse extends EcocashCallbackPayment {
    private String id;
    private String version;
    private String ecocashReference;
    private String serverReferenceCode;
    private String responseCode;
    private Instant startTime;
    private Instant endTime;
    private Object notificationFormat;
    private String serviceId;
    private String originalServerReferenceCode;
    private String orginalMerchantReference;
    private Instant transactionDate;
    private String type;
    private String source;
    private String messageId;
    private String text;

    public static EcocashCallbackPaymentResponse createSimulatedResponse(EcocashCallbackPayment request, String transactionOperationStatus) {
        return (EcocashCallbackPaymentResponse) new EcocashCallbackPaymentResponse()
                .setId(Tool.generateDigits(6, false))
                .setVersion("0")
                .setEcocashReference("SIMULATED." + Tool.generateDigits(10, false))
                .setServerReferenceCode(Tool.generateDigits(12, false) + Tool.generateDigits(12, false))
                .setOrginalMerchantReference(request.getReferenceCode())
                .setType("MP")
                .setSource("RMP_INIT")
                .setStartTime(Instant.now())
                .setEndTime(Instant.now())
                .setTransactionDate(Instant.now())

                .setTranType(request.getTranType())
                .setTransactionOperationStatus(transactionOperationStatus)
                .setEndUserId(request.getEndUserId())
                .setClientCorrelator(request.getClientCorrelator())
                .setReferenceCode(request.getReferenceCode())
                .setNotifyUrl(request.getNotifyUrl())
                .setPaymentAmount(new PaymentAmount()
                        .setCharginginformation(new ChargingInformation()
                                .setAmount(request.getPaymentAmount().getCharginginformation().getAmount())
                                .setCurrency(request.getPaymentAmount().getCharginginformation().getCurrency())
                                .setDescription(request.getPaymentAmount().getCharginginformation().getDescription()))
                        .setChargeMetaData(new ChargeMetaData()
                                .setChannel(request.getPaymentAmount().getChargeMetaData().getChannel())
                                .setPurchaseCategoryCode(request.getPaymentAmount().getChargeMetaData().getPurchaseCategoryCode())
                                .setOnBeHalfOf(request.getPaymentAmount().getChargeMetaData().getOnBeHalfOf())
                                .setServiceId(request.getPaymentAmount().getChargeMetaData().getServiceId()))
                        .setTotalAmountCharged(request.getPaymentAmount().getCharginginformation().getAmount()))
                .setCountryCode(request.getCountryCode())
                .setLocation(request.getLocation())
                .setSuperMerchantName(request.getSuperMerchantName())
                .setMerchantName(request.getMerchantName())
                .setMerchantCode(request.getMerchantCode())
                .setMerchantPin(request.getMerchantPin())
                .setMerchantNumber(request.getMerchantNumber())
                .setRemarks(request.getRemarks());
    }
}
