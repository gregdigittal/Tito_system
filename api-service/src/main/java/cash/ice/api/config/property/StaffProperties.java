package cash.ice.api.config.property;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "ice.cash.staff")
@Component("StaffProperties")
@Data
public class StaffProperties {
    private boolean securityDisabled;
    private int passwordDigitsAmount;
    private boolean passwordSendSms;
    private boolean emailAfterUpdate;
    private boolean emailAfterCreateJournal;
    private String createUserEmailTemplate;
    private String updateUserEmailTemplate;
    private String createJournalEmailTemplate;
    private String updateUserEmailChangeDescriptionTemplate;
    private String activateUserUrl;
    private String forgotPasswordEmailFrom;
    private String notificationsEmailFrom;
    private List<String> createJournalEmailsTo;
    private String forgotPasswordEmailTemplate;
    private String resetPasswordUrl;
    private String reviewJournalUrl;
    private ExportCsv exportCsv;
    private MfaProperties mfa;

    @Data
    @Accessors(chain = true)
    public static class ExportCsv {
        private String fileName;
        private int maxRecords;
    }
}
