package cash.ice.api.service;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.LoginMfaRequest;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.sqldb.entity.LoginStatus;
import org.keycloak.representations.AccessTokenResponse;

public interface StaffMemberLoginService {

    StaffMember registerStaffMember(StaffMember staffMember, String password);

    StaffMember activateNewStaffMember(String key, String newPassword);

    AccessTokenResponse loginFormStaffMember(LoginEntityRequest loginRequest);

    LoginResponse loginStaffMember(LoginEntityRequest loginRequest);

    LoginResponse enterLoginMfaCode(LoginMfaRequest mfaRequest);

    LoginResponse enterLoginMfaBackupCode(LoginMfaRequest mfaRequest);

    boolean checkTotpCode(AuthUser authUser, String totpCode);

    boolean resendOtpCode(String username);

    boolean forgotPassword(String email, String url, boolean sendEmail, String ip);

    StaffMember resetStaffMemberPassword(String key, String newPassword);

    StaffMember updateStaffMemberPassword(AuthUser authUser, String oldPassword, String newPassword);

    StaffMember updateStaffMemberLoginStatus(String email, LoginStatus loginStatus);

    String getMfaQrCode(StaffMember staffMember);
}
