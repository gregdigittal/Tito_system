package cash.ice.api.task;

import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.api.service.EntityRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.database.Database;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Setter
public class RegisterUserDbMigrationTask extends DbMigrationTask {
    private String userJson;

    public void execute(Database database) {
        log.info("> Register user: {}", userJson);
        try {
            EntityRegistrationService entityRegistrationService = applicationContext.getBean(EntityRegistrationService.class);
            ObjectMapper objectMapper = applicationContext.getBean("objectMapper", ObjectMapper.class);
            Map<String, String> map = objectMapper.readValue(userJson, Map.class);
            RegisterEntityRequest registerEntityRequest = objectMapper.readValue(userJson, RegisterEntityRequest.class);
            log.debug("Parsed user: {}", registerEntityRequest);
            entityRegistrationService.registerEntity(
                    registerEntityRequest,
                    map.get("pin"),
                    map.get("key"),
                    map.get("pvv"),
                    map.get("accountNumber"),
                    "ZWL",
                    false);
        } catch (Throwable e) {
            log.error("DbMigrationTask failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "> Migrating Register user finished. Committed.";
    }
}
