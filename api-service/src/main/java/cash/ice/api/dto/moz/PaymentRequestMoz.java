package cash.ice.api.dto.moz;

import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.VendorRefAware;
import cash.ice.common.utils.Tool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class PaymentRequestMoz implements VendorRefAware {

    @NotEmpty(message = "Invalid vendorRef")
    private String vendorRef;

    @NotEmpty(message = "Invalid tx")
    private String tx;

    @NotEmpty(message = "Invalid initiatorType")
    private String initiatorType;

    private String currency;

    @DecimalMin(value = "0.01", message = "Invalid amount")
    @Digits(fraction = 2, integer = 16, message = "Invalid amount")
    private BigDecimal amount;

    @NotEmpty(message = "Invalid apiVersion")
    private String apiVersion;

    @NotNull(message = "Invalid date")
    @DateTimeFormat(pattern = "${ice.cash.date-format}")
    private LocalDateTime date;

    @NotEmpty(message = "Invalid initiator")
    private String initiator;

    private Map<String, Object> meta = new HashMap<>();

    @JsonIgnore
    public Map<String, Object> getMetaData() {
        return meta;
    }

    public void setMetaData(String jsonString) {
        this.meta = Tool.jsonStringToMap(jsonString);
    }

    public PaymentRequest toPaymentRequest() {
        return new PaymentRequest()
                .setVendorRef(vendorRef)
                .setTx(tx)
                .setInitiatorType(initiatorType)
                .setCurrency(currency)
                .setAmount(amount)
                .setApiVersion(apiVersion)
                .setDate(date)
                .setInitiator(initiator)
                .setMeta(meta);
    }
}