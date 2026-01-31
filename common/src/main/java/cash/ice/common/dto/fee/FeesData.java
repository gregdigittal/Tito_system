package cash.ice.common.dto.fee;

import cash.ice.common.dto.PaymentRequest;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class FeesData {
    private PaymentRequest paymentRequest;
    private List<FeeEntry> feeEntries = new ArrayList<>();
    private Integer originalDrAccountId;
    private String vendorRef;
    private String transactionCode;
    private Integer transactionCodeId;
    private Integer currencyId;
    private String currencyCode;
    private Integer initiatorTypeId;
    private String initiatorTypeDescription;
    private Integer initiatorTypeEntityId;
    private String transactionId;
    private BigDecimal balance;
    private BigDecimal subsidyAccountBalance;
    private String primaryMsisdn;
    private Integer initiatorId;
    private Integer initiatorEntityId;
    private String initiatorLocale;
    private Map<String, BigDecimal> limitData;
    private Map<String, Object> metaData;
}
