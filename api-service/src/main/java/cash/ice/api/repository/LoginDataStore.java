package cash.ice.api.repository;

import cash.ice.api.dto.LoginData;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Abstraction for login/MFA session storage so the app can use either MongoDB or in-memory
 * when MongoDB is unavailable (e.g. profile no-mongodb).
 */
public interface LoginDataStore {

    Optional<LoginData> findByLogin(String login);

    LoginData save(LoginData data);

    void saveAll(Iterable<LoginData> data);

    Optional<LoginData> findByForgotPasswordKey(String key);

    List<LoginData> findAllByTokenExpireTimeIsBefore(Instant time);
}
