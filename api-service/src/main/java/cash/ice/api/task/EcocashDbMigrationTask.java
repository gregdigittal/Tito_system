package cash.ice.api.task;

import liquibase.database.Database;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Slf4j
public class EcocashDbMigrationTask extends DbMigrationTask {
    private static final String COLLECTION_NAME = "EcocashMerchant";
    private static final String ZTP_LPP_MERCHANT_JSON = """
            {
                "transactionCodes" : [
                    "ZTP",
                    "LPP"
                ],
                "pin" : "1357",
                "code" : "42467",
                "number" : "771999313"
            }
                                """;
    private static final String GENERAL_MERCHANT_JSON = """
            {
                "general" : true,
                "pin" : "1357",
                "code" : "02273",
                "number" : "771998182"
            }
                                """;

    @Override
    public void execute(Database database) {
        log.debug("> Migrating EcoCash DB");
        try {
            MongoTemplate mongoTemplate = applicationContext.getBean(MONGO_TEMPLATE, MongoTemplate.class);
            if (mongoTemplate.count(query(where("transactionCodes").all(List.of("ZTP", "LPP"))), COLLECTION_NAME) == 0) {
                mongoTemplate.save(ZTP_LPP_MERCHANT_JSON, COLLECTION_NAME);
            }
            if (mongoTemplate.count(query(where("general").is(true)), COLLECTION_NAME) == 0) {
                mongoTemplate.save(GENERAL_MERCHANT_JSON, COLLECTION_NAME);
            }
        } catch (Throwable e) {
            log.warn("{} skipped (Mongo seed optional for minimal deploy): {}", getClass().getSimpleName(), e.getMessage());
            // Do not rethrow: allow app to start without Ecocash Mongo seed (e.g. on Render/Atlas)
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "> Migrating EcoCash DB finished. Committed.";
    }
}
