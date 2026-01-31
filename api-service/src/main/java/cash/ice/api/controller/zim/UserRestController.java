package cash.ice.api.controller.zim;

import cash.ice.api.config.property.EntitiesProperties;
import cash.ice.api.config.property.StaffProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.api.dto.RegisterResponse;
import cash.ice.api.service.*;
import cash.ice.common.dto.UserAuthRegisterResponse;
import cash.ice.common.dto.UserAuthRequest;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.LoginStatus;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserRestController {
    private final EntityRegistrationService entityRegistrationService;
    private final EntityService entityService;
    private final EntityLoginService entityLoginService;
    private final StaffMemberService staffMemberService;
    private final StaffMemberLoginService staffMemberLoginService;
    private final StaffProperties staffProperties;
    private final EntitiesProperties entitiesProperties;
    private final KeycloakService keycloakService;
    private final AuthUserService authUserService;

    @PostMapping("/register")
    @ResponseStatus(code = HttpStatus.CREATED)
    public RegisterResponse registerUser(@Valid @RequestBody RegisterEntityRequest request) {
        try {
            log.info("Received Register new user request: " + request);
            return entityRegistrationService.registerEntity(request);

        } catch (ICEcashException e) {
            log.info("User registration error: " + e.getMessage(), e);
            return RegisterResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    @PostMapping("/login")
    public AccessTokenResponse loginUser(@Valid @RequestBody LoginEntityRequest request) {
        log.info("Received Login request: " + request);
        return entityLoginService.simpleLogin(request);
    }

    @PostMapping(value = "/login/form", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public AccessTokenResponse loginUserForm(@Valid LoginEntityRequest request) {
        log.info("Received Login form request: " + request);
        return entityLoginService.simpleLogin(request);
    }

    @PostMapping(value = "/backoffice/login/form", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public AccessTokenResponse loginStaffMemberForm(@Valid LoginEntityRequest request) {
        log.info("Received Login StaffMember form request: " + request);
        return staffMemberLoginService.loginFormStaffMember(request);
    }

    @PostMapping("/auth")
    @ResponseStatus(code = HttpStatus.CREATED)
    public UserAuthRegisterResponse registerUserAuthentication(@Valid @RequestBody UserAuthRequest request) {
        log.info("Received register user auth request: " + request);
        String keycloakId = keycloakService.createUser(request.getUsername(), request.getPvv(),
                request.getFirstName(), request.getLastName(), request.getEmail());
        return new UserAuthRegisterResponse()
                .setKeycloakId(keycloakId)
                .setTotalUsersCount(keycloakService.getTotalUsersCount());
    }

    @PutMapping("/auth")
    public void updateUserAuthentication(@Valid @RequestBody UserAuthRequest request) {
        log.info("Received update user auth request: " + request);
        keycloakService.updateUser(request.getKeycloakId(), request.getPvv(),
                request.getFirstName(), request.getLastName(), request.getEmail());
    }

    @DeleteMapping("/auth")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteUserAuthentication(@RequestParam String id) {
        log.info("Received delete user auth request. KeycloakId: " + id);
        keycloakService.removeUser(id);
    }

    @GetMapping("/export/csv")
    public void exportUsersToCsv(HttpServletResponse response,
                                 @RequestParam(required = false) String searchText,
                                 @RequestParam(required = false) LoginStatus status,
                                 @RequestParam(required = false) boolean header,
                                 @RequestParam(required = false) Character delimiter,
                                 @RequestParam(required = false) String rowDelimiter) throws IOException {
        log.debug("> GET users csv, searchText: {}, status: {}, header: {}, delimiter: {}, rowDelimiter: {}", searchText, status, header, delimiter, rowDelimiter);
        String usersCsv = staffMemberService.getUsersCsv(searchText, status, header, delimiter, rowDelimiter);
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + staffProperties.getExportCsv().getFileName());
        response.setContentLength(usersCsv.getBytes().length);
        IOUtils.copy(new ByteArrayInputStream(usersCsv.getBytes()), response.getOutputStream());
    }

    @GetMapping("/account/{accountType}/{currencyCode}/statement/export/csv")
    @PreAuthorize("@EntitiesProperties.securityDisabled || isAuthenticated()")
    public void exportUsersToCsv(HttpServletResponse response,
                                 @PathVariable String accountType,
                                 @PathVariable String currencyCode,
                                 @RequestParam(required = false) boolean header,
                                 @RequestParam(required = false) Character delimiter,
                                 @RequestParam(required = false) String rowDelimiter) throws IOException {
        EntityClass authEntity = entityService.getEntity(authUserService.getAuthUser());
        log.debug("> GET statement csv, accountType: {}, currencyCode: {}, user: {}, header: {}, delimiter: {}, rowDelimiter: {}",
                accountType, currencyCode, authEntity, header, delimiter, rowDelimiter);
        String statementCsv = entityService.getStatementCsv(authEntity, accountType, currencyCode, header, delimiter, rowDelimiter);
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + entitiesProperties.getStatementExportCsv().getFileName());
        response.setContentLength(statementCsv.getBytes().length);
        IOUtils.copy(new ByteArrayInputStream(statementCsv.getBytes()), response.getOutputStream());
    }

    @GetMapping("/sme")
    @PreAuthorize("isAuthenticated()")
    public AuthUser getAuthUserSecured() {
        log.info("me: " + authUserService.getAuthUser());
        return authUserService.getAuthUser();
    }

    @GetMapping("/me")
    public AuthUser getAuthUser() {
        log.info("me: " + authUserService.getAuthUser());
        return authUserService.getAuthUser();
    }
}
