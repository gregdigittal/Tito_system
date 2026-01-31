package cash.ice.api.task;

import liquibase.database.Database;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Slf4j
public class FlexcubeDbMigrationTask extends DbMigrationTask {
    private static final String COLLECTION_NAME = "FlexcubeAccount";
    private static final String ACCOUNT_JSON = """
            {
                "transactionCodes" : [
                    "TRN"
                ],
                "debitPoolAccount" : "6132741250115",
                "debitPoolAccountBranch" : "003",
                "transactionFee" : 1.00,
                "minBalanceMargin" : 100.00,
                "balanceWarningValue" : 200.00
            }
            """;

    @Override
    public void execute(Database database) {
        log.debug("> Migrating Flexcube DB");
        try {
            MongoTemplate mongoTemplate = applicationContext.getBean(MONGO_TEMPLATE, MongoTemplate.class);
            if (mongoTemplate.count(query(where("transactionCodes").all(List.of("TRN"))), COLLECTION_NAME) == 0) {
                mongoTemplate.save(ACCOUNT_JSON, COLLECTION_NAME);
            }
        } catch (Throwable e) {
            log.warn("{} skipped (Mongo seed optional for minimal deploy): {}", getClass().getSimpleName(), e.getMessage());
            // Do not rethrow: allow app to start without Flexcube Mongo seed (e.g. on Render/Atlas)
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "> Migrating Flexcube DB finished. Committed.";
    }
}
