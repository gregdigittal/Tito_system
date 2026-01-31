package cash.ice.api.service;

import org.keycloak.representations.AccessTokenResponse;

import java.util.List;

public interface KeycloakService {

    String createStaffMember(String entityId, String password, String firstName, String lastName, String email);

    String createUser(String entityId, String password, String firstName, String lastName, String email);

    void updateUser(String keycloakId, String password, String firstName, String lastName, String email);

    void updateStaffMemberUsername(String keycloakId, String email);

    void removeUser(String keycloakId);

    void addRolesToUser(String keycloakId, List<String> roles);

    void removeRolesFromUser(String keycloakId, List<String> roles);

    Integer getTotalUsersCount();

    AccessTokenResponse loginStaffMember(String entityId, String password);

    AccessTokenResponse loginUser(String grantType, String entityId, String password, String clientId, String clientSecret);

    AccessTokenResponse refreshToken(String refreshToken, String clientId, String clientSecret);

    void invalidateRefreshToken(String refreshToken, String clientId, String clientSecret);
}
