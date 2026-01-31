package cash.ice.api.service;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.LoginMfaRequest;
import cash.ice.api.dto.LoginResponse;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.LoginStatus;
import cash.ice.sqldb.entity.MfaType;
import org.keycloak.representations.AccessTokenResponse;

public interface EntityLoginService {

    AccessTokenResponse simpleLogin(LoginEntityRequest request);

    LoginResponse makeLogin(LoginEntityRequest request);

    default LoginResponse makePosDeviceLogin(String deviceSerial, LoginEntityRequest loginEntityRequest) {
        throw new UnsupportedOperationException();
    }

    LoginResponse enterLoginMfaCode(LoginMfaRequest mfaRequest);

    LoginResponse enterLoginMfaBackupCode(LoginMfaRequest mfaRequest);

    boolean checkTotpCode(AuthUser authUser, String totpCode);

    boolean resendOtpCode(String username);

    boolean forgotPassword(String email, boolean sendEmail, String requestIP);

    EntityClass resetEntityPassword(String key, String newPassword);

    EntityClass updateEntityMfa(Integer entityId, MfaType mfaType);

    EntityClass generateNewEntityPassword(Integer entityId);

    EntityClass updateEntityPassword(AuthUser authUser, String oldPassword, String newPassword);

    EntityClass updateEntityLoginStatus(String enterId, LoginStatus loginStatus);

    String getMfaQrCode(EntityClass entity);

    EntityClass findEntity(String enterId);
}
