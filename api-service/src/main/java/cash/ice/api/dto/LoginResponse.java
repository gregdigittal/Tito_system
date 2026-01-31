package cash.ice.api.dto;

import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.MfaType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;
import org.keycloak.representations.AccessTokenResponse;

import java.util.Locale;

@Data
@Accessors(chain = true)
public class LoginResponse {
    private String login;
    private Status status;
    private MfaType mfaType;
    private String msisdn;
    private Locale locale;
    private AccessTokenResponse accessToken;
    @JsonIgnore
    private EntityClass entity;

    public static LoginResponse success(AccessTokenResponse accessToken) {
        return new LoginResponse()
                .setStatus(Status.SUCCESS)
                .setAccessToken(accessToken);
    }

    public static LoginResponse successNeedMfa(String login, MfaType neededMfaType, String msisdn) {
        return new LoginResponse()
                .setLogin(login)
                .setStatus(Status.MFA_REQUIRED)
                .setMfaType(neededMfaType)
                .setMsisdn(msisdn);
    }

    public enum Status {
        SUCCESS, MFA_REQUIRED
    }
}
