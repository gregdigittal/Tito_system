package cash.ice.api.service.impl;

import cash.ice.api.config.property.KeycloakProperties;
import cash.ice.api.errors.RegistrationException;
import cash.ice.api.service.KeycloakService;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.performance.PerfStopwatch;
import com.google.common.base.Strings;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.admin.client.token.TokenService;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.List;

import static cash.ice.common.error.ErrorCodes.EC1004;
import static cash.ice.common.error.ErrorCodes.EC1008;
import static cash.ice.common.utils.Tool.*;
import static org.keycloak.OAuth2Constants.*;

@RequiredArgsConstructor
@Slf4j
public class KeycloakServiceImpl implements KeycloakService {
    private final KeycloakProperties keycloakProperties;
    private final String userNamePrefix;

    private Keycloak adminKeycloak;
    private UsersResource usersResource;
    private RolesResource rolesResource;
    private PerfStopwatch createPerfStopwatch;
    private PerfStopwatch updatePerfStopwatch;

    public static AccessTokenResponse logToken(AccessTokenResponse token) {
        if (token != null) {
            log.debug("  new token: ({}){}...{} ttl: {}, refresh: ({}){}...{} ttl: {}{}",
                    checksum(token.getToken()), substrFirst(30, token.getToken()), substrLast(30, token.getToken()), token.getExpiresIn(),
                    checksum(token.getRefreshToken()), substrFirst(30, token.getRefreshToken()), substrLast(30, token.getRefreshToken()), token.getRefreshExpiresIn(),
                    token.getError() != null ? ", error: " + token.getError() + ", errorDescription: " + token.getErrorDescription() : "");
        }
        return token;
    }

    @PostConstruct
    public void init() {
        adminKeycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakProperties.getAuthServerUrl())
                .realm(keycloakProperties.getRealm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(keycloakProperties.getAdminClientId())
                .clientSecret(keycloakProperties.getAdminClientSecret()).build();
        RealmResource realmResource = adminKeycloak.realm(keycloakProperties.getRealm());
        usersResource = realmResource.users();
        rolesResource = realmResource.roles();
        createPerfStopwatch = new PerfStopwatch();
        updatePerfStopwatch = new PerfStopwatch();
    }

    @Override
    public String createStaffMember(String entityId, String password, String firstName, String lastName, String email) {
        return createUser(userNamePrefix, entityId, password, firstName, lastName, email);
    }

    @Override
    public String createUser(String entityId, String password, String firstName, String lastName, String email) {
        return createUser(userNamePrefix, entityId, password, firstName, lastName, email);
    }

    private String createUser(String usernamePrefix, String entityId, String password, String firstName, String lastName, String email) {
        String username = usernamePrefix + entityId;
        log.debug("  Keycloak create user: {}, firstName: {}, lastName: {}, email: {}. Total users: {}",
                username, firstName, lastName, email, getTotalUsersCount());
        createPerfStopwatch.start();
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setCredentials(Collections.singletonList(createPasswordCredentials(password)));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(false);
        fixUserRepresentationIfNeed(user);

        try (Response response = usersResource.create(user)) {
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new RegistrationException(EC1008, "Keycloak error: " + response.getStatusInfo().getStatusCode() + " "
                        + response.getStatusInfo().getReasonPhrase());
            }
            createPerfStopwatch.stop();
            log.debug("  Keycloak user created: {}, time: {}", username, createPerfStopwatch);
            String userIdPath = response.getLocation().getPath();
            return userIdPath.substring(userIdPath.lastIndexOf("/") + 1);
        }
    }

    @Override
    public void updateUser(String keycloakId, String password, String firstName, String lastName, String email) {
        log.debug("  Keycloak update user: {},  total users: {}", keycloakId, getTotalUsersCount());
        updatePerfStopwatch.start();
        UserResource userResource = getUser(keycloakId);
        if (userResource == null) {
            throw new IllegalArgumentException(String.format("Keycloak user with id=%s does not exist", keycloakId));
        }
        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setCredentials(Collections.singletonList(createPasswordCredentials(password)));
        userRepresentation.setFirstName(firstName);
        userRepresentation.setLastName(lastName);
        userRepresentation.setEmail(email);
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(false);
        fixUserRepresentationIfNeed(userRepresentation);
        userResource.update(userRepresentation);
        updatePerfStopwatch.stop();
        log.debug("  Keycloak user updated: {}, time: {}", keycloakId.substring(0, 3), updatePerfStopwatch);
    }

    private void fixUserRepresentationIfNeed(UserRepresentation user) {
        if (user.getFirstName() == null) {
            user.setFirstName("-");
        }
        if (user.getLastName() == null) {
            user.setLastName("-");
        }
        if (user.getEmail() == null) {
            user.setEmail(String.format("%s.%s@ice.cash", fixEmailStr(user.getFirstName()), fixEmailStr(user.getLastName())));
        }
    }

    private String fixEmailStr(String str) {
        return str.replaceAll("[^a-zA-Z0-9_\\-]", "");
    }

    @Override
    public void updateStaffMemberUsername(String keycloakId, String email) {
        log.debug("  Keycloak update staff member username: {}, email: {}, new username: {},  total users: {}", keycloakId, email, userNamePrefix + email, getTotalUsersCount());
        updatePerfStopwatch.start();
        UserResource userResource = getUser(keycloakId);
        if (userResource == null) {
            throw new IllegalArgumentException(String.format("Keycloak user with id=%s does not exist", keycloakId));
        }
        UserRepresentation userRepresentation = userResource.toRepresentation();
//        userRepresentation.setUsername(userNamePrefix + email);                   // HTTP 400 Bad Request from Keycloak 24
        userRepresentation.setEmail(email);
        fixUserRepresentationIfNeed(userRepresentation);
        userResource.update(userRepresentation);
        updatePerfStopwatch.stop();
        log.debug("  Keycloak user updated: {}, time: {}", keycloakId.substring(0, 3), updatePerfStopwatch);
    }

    @Override
    public void removeUser(String keycloakId) {
        log.debug("  Keycloak remove user: {}", keycloakId);
        UserResource user = getUser(keycloakId);
        if (user != null) {
            user.remove();
        } else {
            log.warn("Cannot remove user {}. User wasn't found!", keycloakId);
        }
    }

    @Override
    public void addRolesToUser(String keycloakId, List<String> roles) {
        addRemoveRolesToUser(keycloakId, roles, true);
    }

    @Override
    public void removeRolesFromUser(String keycloakId, List<String> roles) {
        addRemoveRolesToUser(keycloakId, roles, false);
    }

    @Override
    public Integer getTotalUsersCount() {
        try {
            return usersResource.count();
        } catch (Exception e) {
            log.error("Cannot get users count", e);
            return null;
        }
    }

    @Override
    public AccessTokenResponse loginStaffMember(String entityId, String password) {
        return loginUser(userNamePrefix, null, entityId, password, null, null);
    }

    @Override
    public AccessTokenResponse loginUser(String grantType, String entityId, String password, String clientId, String clientSecret) {
        return loginUser(userNamePrefix, grantType, entityId, password, clientId, clientSecret);
    }

    private AccessTokenResponse loginUser(String usernamePrefix, String grantType, String entityId, String password, String clientId, String clientSecret) {
        String username = usernamePrefix + entityId;
        String effectiveGrantType = Strings.isNullOrEmpty(grantType) ? OAuth2Constants.PASSWORD : grantType;
        String clientID = Strings.isNullOrEmpty(clientId) ? keycloakProperties.getDefaultClientId() : clientId;
        String clientSecretToUse = Strings.isNullOrEmpty(clientSecret) ? keycloakProperties.getDefaultClientSecret() : clientSecret;
        String realm = keycloakProperties.getRealm();
        String url = keycloakProperties.getAuthServerUrl();

        if (Strings.isNullOrEmpty(clientID) || clientSecretToUse == null) {
            log.error("KEYCLOAK_LOGIN_FAILED config missing: clientId={}, clientSecretSet={}, realm={}, url={}. Set ICE_CASH_KEYCLOAK_ENTITIES_DEFAULT_CLIENT_ID and ICE_CASH_KEYCLOAK_ENTITIES_DEFAULT_CLIENT_SECRET (or AUTH_KEYCLOAK_ENTITIES_*) on Render.",
                    clientID, clientSecretToUse != null, realm, url);
        }
        log.info("KEYCLOAK_LOGIN_ATTEMPT username={}, realm={}, clientId={}, url={}", username, realm, clientID, url);

        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(url)
                .realm(realm)
                .grantType(effectiveGrantType)
                .clientId(clientID)
                .clientSecret(clientSecretToUse)
                .username(username)
                .password(password)
                .build()) {
            AccessTokenResponse token = keycloak.tokenManager().getAccessToken();
            if (token == null || token.getToken() == null) {
                log.error("KEYCLOAK_LOGIN_FAILED no token returned for username={}, realm={}, clientId={}, url={}", username, realm, clientID, url);
            }
            return token;
        } catch (Exception e) {
            log.error("KEYCLOAK_LOGIN_FAILED username={}, realm={}, clientId={}, url={} exception={}: {}", username, realm, clientID, url, e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public AccessTokenResponse refreshToken(String refreshToken, String clientId, String clientSecret) {
        try (Client client = Keycloak.getClientProvider().newRestEasyClient(null, null, false)) {
            TokenService tokenService = getTokenService(client, clientId, clientSecret);
            Form form = new Form().param(GRANT_TYPE, REFRESH_TOKEN)
                    .param(REFRESH_TOKEN, refreshToken);
            form.param(CLIENT_ID, Strings.isNullOrEmpty(clientId) ? keycloakProperties.getDefaultClientId() : clientId);
            try {
                return tokenService.refreshToken(keycloakProperties.getRealm(), form.asMap());
            } catch (Exception e) {
                log.warn("[{}] Refresh token failed, message: {}, realm: {}, map: {}", e.getClass().getName(), e.getMessage(), keycloakProperties.getRealm(), form.asMap());
                throw new ICEcashException("Refresh token failed", EC1004);
            }
        }
    }

    @Override
    public void invalidateRefreshToken(String refreshToken, String clientId, String clientSecret) {
        try (Client client = Keycloak.getClientProvider().newRestEasyClient(null, null, false)) {
            TokenService tokenService = getTokenService(client, clientId, clientSecret);
            Form form = new Form().param(REFRESH_TOKEN, refreshToken);
            form.param(CLIENT_ID, Strings.isNullOrEmpty(clientId) ? keycloakProperties.getDefaultClientId() : clientId);
            tokenService.logout(keycloakProperties.getRealm(), form.asMap());
        }
    }

    private TokenService getTokenService(Client client, String clientId, String clientSecret) {
        WebTarget target = client.target(keycloakProperties.getAuthServerUrl());
        if (clientSecret != null) {
            target.register(new BasicAuthFilter(clientId, clientSecret));
        }
        return Keycloak.getClientProvider().targetProxy(target, TokenService.class);
    }

    private void addRemoveRolesToUser(String keycloakId, List<String> roles, boolean add) {
        log.debug("  Keycloak {} roles: {} user: {}", add ? "add" : "remove", roles, keycloakId);
        UserResource user = getUser(keycloakId);
        if (user != null) {
            List<RoleRepresentation> roleRepresentations = getRoleRepresentations(roles);
            if (!roleRepresentations.isEmpty()) {
                if (add) {
                    user.roles().realmLevel().add(roleRepresentations);
                } else {
                    user.roles().realmLevel().remove(roleRepresentations);
                }
            } else {
                log.warn("Roles {} doesn't exist!", roles);
            }
        } else {
            log.warn("Cannot {} roles to user. User {} does not exist!", add ? "add" : "remove", keycloakId);
        }
    }

    private UserResource getUser(String keycloakId) {
        return usersResource.get(keycloakId);
    }

    private List<RoleRepresentation> getRoleRepresentations(List<String> roles) {
        return rolesResource.list().stream()
                .filter(role -> roles.contains(role.getName()))
                .toList();
    }

    private CredentialRepresentation createPasswordCredentials(String password) {
        CredentialRepresentation passwordCredentials = new CredentialRepresentation();
        passwordCredentials.setTemporary(false);
        passwordCredentials.setType(CredentialRepresentation.PASSWORD);
        passwordCredentials.setValue(password);
        return passwordCredentials;
    }

    @PreDestroy
    public void preDestroy() {
        adminKeycloak.close();
    }
}
