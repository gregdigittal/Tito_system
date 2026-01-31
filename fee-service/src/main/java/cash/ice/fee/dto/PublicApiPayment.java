package cash.ice.fee.dto;

import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.VendorRefAware;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PublicApiPayment implements VendorRefAware {
    @NotEmpty
    private String apiVersion;
    @NotEmpty
    private String partnerId;
    @NotNull
    @DateTimeFormat(pattern = "${ice.cash.date-format}")
    private LocalDateTime date;
    @NotEmpty
    private String vendorRef;
    @NotEmpty
    private String tx;
    @DecimalMin(value = "0.01")
    @Digits(fraction = 2, integer = 16)
    private BigDecimal amount;

    @NotEmpty
    private String initiator;
    @NotEmpty
    private String initiatorType;
    private String initiatorPVV;
    private String initiatorTrack2;
    private String initiatorIccData;

    private String deviceId;
    private String deviceUser;

    private String additionalParameters;
    private String currency;
    private Integer tourismLevyInd;

    public PaymentRequest toApiPayment() {
        return new PaymentRequest()
                .setApiVersion(apiVersion)
                .setPartnerId(partnerId)
                .setDate(date)
                .setVendorRef(vendorRef)
                .setTx(tx)
                .setAmount(amount)
                .setInitiator(initiator)
                .setInitiatorType(initiatorType)
                .setInitiatorPVV(initiatorPVV)
                .setInitiatorTrack2(initiatorTrack2)
                .setInitiatorIccData(initiatorIccData)
                .setDeviceId(deviceId)
                .setDeviceUser(deviceUser)
                .setAdditionalParameters(additionalParameters)
                .setCurrency(currency)
                .setTourismLevyInd(tourismLevyInd);
    }
}
