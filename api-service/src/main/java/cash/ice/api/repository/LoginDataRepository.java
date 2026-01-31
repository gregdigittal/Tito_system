package cash.ice.api.repository;

import cash.ice.api.dto.LoginData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoginDataRepository extends MongoRepository<LoginData, String> {

    Optional<LoginData> findByLogin(String login);

    Optional<LoginData> findByForgotPasswordKey(String forgotPasswordKey);

    List<LoginData> findAllByTokenExpireTimeIsBefore(Instant tokenReceiveTime);
}
