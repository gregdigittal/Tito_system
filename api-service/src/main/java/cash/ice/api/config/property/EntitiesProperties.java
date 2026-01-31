package cash.ice.api.config.property;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ice.cash.entities")
@Component("EntitiesProperties")
@Data
public class EntitiesProperties {
    private boolean securityDisabled;
    private boolean validateEmailUniqueness;
    private boolean validatePhoneUniqueness;
    private boolean validateIdUniqueness;
    private int passwordDigitsAmount;
    private boolean passwordSendSms;
    private String forgotPasswordEmailFrom;
    private String forgotPasswordEmailTemplate;
    private MfaProperties mfa;
    private StatementExportCsv statementExportCsv;

    @Data
    @Accessors(chain = true)
    public static class StatementExportCsv {
        private String fileName;
        private int maxDays;
        private int maxRecords;
    }
}
