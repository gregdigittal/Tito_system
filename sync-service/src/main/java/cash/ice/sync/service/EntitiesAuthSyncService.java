package cash.ice.sync.service;

import cash.ice.common.dto.UserAuthRegisterResponse;
import cash.ice.common.dto.UserAuthRequest;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class EntitiesAuthSyncService implements DataMigrator {
    private final EntityRepository entityRepository;
    private final RestTemplate restTemplate;

    @Value("${ice.cash.user-auth-url}")
    private String userAuthUrl;

    @Override
    public void migrateData() {
        log.debug("Start registering entities auth data. AuthUrl: " + userAuthUrl);
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger totalUsers = new AtomicInteger(0);
        entityRepository.findAll().forEach(entity -> {
            if (entity.getKeycloakId() == null && !ObjectUtils.isEmpty(entity.getInternalId()) && !ObjectUtils.isEmpty(entity.getPvv())) {
                if (counter.incrementAndGet() % 5000 == 0) {
                    log.debug("  {} accounts processed, total: {}", counter.get(), totalUsers.get());
                }
                Integer totalUsersCount = createAuth(entity);
                totalUsers.set(totalUsersCount != null ? totalUsersCount : 0);
            }
        });
        log.info("Finished registering entities auth data: {} processed", counter.get());
    }

    @Transactional
    public Integer createAuth(EntityClass entity) {
        UserAuthRequest userAuthRequest = createUserAuthRequest(entity);
        ResponseEntity<UserAuthRegisterResponse> response = restTemplate.postForEntity(userAuthUrl,
                userAuthRequest,
                UserAuthRegisterResponse.class);
        if (response.getStatusCodeValue() == 201 && response.getBody() != null) {
            try {
                entityRepository.save(entity.setKeycloakId(response.getBody().getKeycloakId()));
                return response.getBody().getTotalUsersCount();
            } catch (Exception e) {
                deleteAuth(response.getBody().getKeycloakId());
                throw e;
            }
        } else {
            log.error("Error! authRequest: " + userAuthRequest + ", entity: " + entity);
            throw new IllegalStateException(String.format("Cannot create auth for entity: %s, statusCode: %s",
                    entity, response.getStatusCode()));
        }
    }

    private UserAuthRequest createUserAuthRequest(EntityClass entity) {
        return new UserAuthRequest()
                .setKeycloakId(entity.getKeycloakId())
                .setUsername(entity.keycloakUsername())
                .setNumber(entity.getInternalId())
                .setPvv(entity.getPvv())
                .setFirstName(entity.getFirstName())
                .setLastName(entity.getLastName())
                .setEmail(entity.getEmail());
    }

    private void deleteAuth(String keycloakId) {
        if (keycloakId != null) {
            restTemplate.delete(userAuthUrl + "?id={id}", Map.of("id", keycloakId));
        }
    }

    public void update(DataChange dataChange, EntityClass entity) {
        if (dataChange.getAction() == ChangeAction.DELETE) {
            deleteAuth(entity.getKeycloakId());
        } else {
            if (dataChange.getData().keySet().stream().anyMatch(key ->
                    List.of("ID_Number_Converted", "PVV", "First_Name", "Last_Name", "Email").contains(key))) {
                if (entity.getKeycloakId() == null) {
                    if (!ObjectUtils.isEmpty(entity.getInternalId()) && !ObjectUtils.isEmpty(entity.getPvv())) {
                        createAuth(entity);
                    }
                } else {
                    restTemplate.put(userAuthUrl, createUserAuthRequest(entity));
                }
            }
        }
    }
}
