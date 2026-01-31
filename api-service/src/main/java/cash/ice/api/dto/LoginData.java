package cash.ice.api.dto;

import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.MfaType;
import lombok.Data;
import lombok.experimental.Accessors;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "LoginData")
@Data
@Accessors(chain = true)
public class LoginData {

    @Id
    private String id;
    private String login;
    private MfaType mfaType;
    private String mfaPinKey;
    private String mfaSecretCode;
    private AccessTokenResponse token;
    private Instant tokenReceiveTime;
    private Instant tokenExpireTime;
    private String otpPvv;
    private String forgotPasswordKey;
    private List<LocalDateTime> failedLoginAttempts;

    public LoginData cleanup(boolean removeFailedLoginAttempts) {
        mfaType = null;
        mfaPinKey = null;
        mfaSecretCode = null;
        token = null;
        tokenReceiveTime = null;
        tokenExpireTime = null;
        otpPvv = null;
        if (removeFailedLoginAttempts) {
            failedLoginAttempts = null;
        }
        return this;
    }

    public LoginData addFailedLoginAttempt(int maxAttempts) {
        if (failedLoginAttempts == null) {
            failedLoginAttempts = new ArrayList<>();
        }
        failedLoginAttempts.add(Tool.currentDateTime());
        while (failedLoginAttempts.size() > maxAttempts) {
            failedLoginAttempts.remove(0);
        }
        return this;
    }

    public Duration getDurationBetweenFailedLoginAttempts() {
        return failedLoginAttempts == null || failedLoginAttempts.size() < 2 ? Duration.ZERO :
                Duration.between(failedLoginAttempts.get(0),
                        failedLoginAttempts.get(failedLoginAttempts.size() - 1));
    }

    public boolean isTokenAbsentOrExpired() {
        return token == null || tokenExpireTime == null || tokenExpireTime.isBefore(Instant.now());
    }

    public boolean isOtpCodeExpired(Duration otpExpiration) {
        return otpExpiration != null && tokenReceiveTime.plus(otpExpiration).isBefore(Instant.now());
    }
}
