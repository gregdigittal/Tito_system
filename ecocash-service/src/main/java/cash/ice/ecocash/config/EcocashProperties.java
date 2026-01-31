package cash.ice.ecocash.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@ConfigurationProperties(prefix = "ice.cash.ecocash")
@Component
@Data
public class EcocashProperties {
    private String paymentUrl;
    private String paymentStatusUrl;
    private String refundUrl;
    private String notifyUrlHost;
    private String httpAuthUser;
    private String httpAuthPass;
    private String phoneExpression;
    private String requestCountryCode;
    private String requestLocation;
    private String requestSuperMerchantName;
    private String requestMerchantName;
    private String paymentAmountChannel;
    private String paymentAmountPurchaseCategoryCode;
    private String paymentAmountOnBehalfOf;
    private Integer statusPollInitDelay;
    private Integer statusPollTimeout;
    private Integer cleanupHistoryOlderDays;
    private boolean expiredPaymentsRecheck;
    private long expiredPaymentsRecheckMs;
    private int expiredPaymentsRecheckAfter;
    private Map<String, String> simulateResponse;

    public String getPaymentStatusUrl(String msisdn, String ref) {
        return String.format(paymentStatusUrl, msisdn, ref);
    }
}
