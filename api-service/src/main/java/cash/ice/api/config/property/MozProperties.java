package cash.ice.api.config.property;

import cash.ice.api.dto.moz.AccountTypeMoz;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "ice.cash.moz")
@Component("MozProperties")
@Data
public class MozProperties {
    private boolean securityDisabled;
    private boolean validateEmailUniqueness;
    private boolean validatePhoneUniqueness;
    private boolean validateIdUniqueness;
    private boolean validateCompanyUniqueness;
    private boolean validateCompanyNuelUniqueness;
    private boolean userRegCheckOtp;
    private boolean entityRegByStaffCheckOtp;
    private int newUserSecurityGroupId;
    private String suspenseAccountNumber;
    private String subsidyPoolAccountNumber;
    private String fndsSuspenseAccountNumber;
    private String photoBucketName;
    private String photoBaseFolder;
    private boolean registerNotificationSmsEnable;
    private String registerNotificationSmsMessageEn;
    private String registerNotificationSmsMessagePt;
    private Duration paymentTimeoutDuration;
    private boolean paymentConfirmationSmsEnable;
    private String paymentConfirmationSmsMessageEn;
    private String paymentConfirmationSmsMessagePt;
    private String offloadFailTicketSubject;
    private String offloadFailTicketBody;
    private int offloadFailTicketPriority;
    private int deviceCodeCharacters;
    private boolean vehicleValidateOwner;
    private boolean linkPosValidateAgent;
    private boolean linkPosValidateOwner;
    private boolean linkTagCheckOtp;
    private boolean linkTagValidateDevice;
    private String linkTagDataCollection;
    private int linkTagOtpDigitsAmount;
    private Duration linkTagRequestExpirationDuration;
    private String linkTagExpiredRequestsCleanupCron;
    private boolean linkPosCheckOtp;
    private List<AccountTypeMoz> agentRegularRegisterPermissions;
    private List<AccountTypeMoz> agentFematroRegisterPermissions;
    private String regAgreementPrefix;
    private boolean simulateTopupAccount;
}
