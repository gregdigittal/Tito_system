package cash.ice.api.repository;

import cash.ice.api.dto.LoginData;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnBean(LoginDataRepository.class)
public class MongoLoginDataStore implements LoginDataStore {

    private final LoginDataRepository repository;

    @Override
    public Optional<LoginData> findByLogin(String login) {
        return repository.findByLogin(login);
    }

    @Override
    public LoginData save(LoginData data) {
        return repository.save(data);
    }

    @Override
    public void saveAll(Iterable<LoginData> data) {
        repository.saveAll(data);
    }

    @Override
    public Optional<LoginData> findByForgotPasswordKey(String key) {
        return repository.findByForgotPasswordKey(key);
    }

    @Override
    public List<LoginData> findAllByTokenExpireTimeIsBefore(Instant time) {
        return repository.findAllByTokenExpireTimeIsBefore(time);
    }
}
