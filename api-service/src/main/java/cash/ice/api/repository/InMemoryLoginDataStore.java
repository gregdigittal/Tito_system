package cash.ice.api.repository;

import cash.ice.api.dto.LoginData;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory LoginData store for when MongoDB is not available (profile no-mongodb).
 * Session data is lost on restart and not shared across instances. Use only for
 * temporary/testing so login works with TiDB + Keycloak only.
 */
@Slf4j
public class InMemoryLoginDataStore implements LoginDataStore {

    private final Map<String, LoginData> byLogin = new ConcurrentHashMap<>();
    private final Map<String, LoginData> byForgotPasswordKey = new ConcurrentHashMap<>();

    @Override
    public Optional<LoginData> findByLogin(String login) {
        return Optional.ofNullable(byLogin.get(login));
    }

    @Override
    public LoginData save(LoginData data) {
        if (data.getLogin() != null) {
            byLogin.put(data.getLogin(), data);
        }
        if (data.getForgotPasswordKey() != null) {
            byForgotPasswordKey.put(data.getForgotPasswordKey(), data);
        } else {
            byForgotPasswordKey.values().removeIf(v -> data.getLogin() != null && data.getLogin().equals(v.getLogin()));
        }
        return data;
    }

    @Override
    public void saveAll(Iterable<LoginData> data) {
        data.forEach(this::save);
    }

    @Override
    public Optional<LoginData> findByForgotPasswordKey(String key) {
        return Optional.ofNullable(byForgotPasswordKey.get(key));
    }

    @Override
    public List<LoginData> findAllByTokenExpireTimeIsBefore(Instant time) {
        List<LoginData> out = new ArrayList<>();
        for (LoginData d : byLogin.values()) {
            if (d.getTokenExpireTime() != null && d.getTokenExpireTime().isBefore(time)) {
                out.add(d);
            }
        }
        return out;
    }
}
