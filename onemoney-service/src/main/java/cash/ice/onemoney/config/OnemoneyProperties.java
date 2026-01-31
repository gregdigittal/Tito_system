package cash.ice.onemoney.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@ConfigurationProperties(prefix = "ice.cash.onemoney")
@Component
@Data
@Accessors(chain = true)
public class OnemoneyProperties {
    private String group;
    private String mockServerUrl;
    private boolean useMockServer;
    private String paymentUrl;
    private String statusUrl;
    private String reversalUrl;
    private long statusPollCheckMs;
    private int statusPollInitDelay;
    private int paymentTimeout;
    private String cleanupHistoryCron;
    private int cleanupHistoryOlderDays;
    private boolean expiredPaymentsRecheck;
    private long expiredPaymentsRecheckMs;
    private int expiredPaymentsRecheckAfter;
    private Request request;
    private Result result;
    private Map<String, String> simulateResponse;

    @Data
    @Accessors(chain = true)
    public static class Request {
        private String headerVersion;
        private int callerType;
        private String thirdPartyId;
        private String password;
        private int keyOwner;
        private String timestampPattern;
        private int onemoneyInitiatorIdentifierType;
        private String onemoneyInitiatorIdentifier;
        private String onemoneySecurityCredential;
        private int initiatorIdentifierType;
        private int receiverIdentifierType;
        private String receiverIdentifier;
        private String defaultRemark;
    }

    @Data
    @Accessors(chain = true)
    public static class Result {
        private String urlPrefix;
        private String locationUri;
        private String portTypeName;
    }
}
