package cash.ice.common.dto;

import cash.ice.common.utils.Tool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentRequest implements VendorRefAware {

    @Id
    @NotEmpty(message = "'vendorRef' must not be empty and must be unique")
    private String vendorRef;

    @NotEmpty(message = "'apiVersion' must not be empty")
    private String apiVersion;
    private String partnerId;

    @NotNull(message = "'date' must not be empty")
    @DateTimeFormat(pattern = "${ice.cash.date-format}")
    private LocalDateTime date;

    @NotEmpty(message = "'tx' must not be empty")
    private String tx;
    @DecimalMin(value = "0.01", message = "0.01 is minimal value for 'amount'")
    @Digits(fraction = 2, integer = 16, message = "'amount' length must be 16 digits max and contain 2 decimal digits after the point (eg. 123.02)")
    private BigDecimal amount;

    @NotEmpty(message = "'initiator' must not be empty")
    private String initiator;
    @NotEmpty(message = "'initiatorType' must not be empty")
    private String initiatorType;
    private String initiatorPVV;
    private String initiatorTrack2;
    private String initiatorIccData;

    private String deviceId;
    private String deviceUser;
    private Map<String, Object> meta = new HashMap<>();

    private String additionalParameters;
    private String currency;
    private Integer tourismLevyInd;

    private Integer result;
    private String message;
    private Integer transactionId;
    private BigDecimal balance;

    @JsonIgnore
    public Map<String, Object> getMetaData() {
        return meta;
    }

    public void setMetaData(String jsonString) {
        this.meta = Tool.jsonStringToMap(jsonString);
    }

    public void addMetaData(String key, Object value) {
        if (meta == null) {
            meta = new HashMap<>();
        } else {
            meta = new HashMap<>(meta);
        }
        meta.put(key, value);
    }

    public boolean containsMetaKey(String metaDataKey) {
        return meta != null && meta.containsKey(metaDataKey);
    }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "vendorRef=" + vendorRef +
                ", tx='" + tx +
                ", initiatorType='" + initiatorType +
                ", initiator='" + initiator +
                ", currency='" + currency +
                ", amount=" + amount +
                ", partnerId='" + partnerId +
                ", apiVersion='" + apiVersion +
                ", date=" + date +
                ", meta=" + meta +
                '}';
    }
}