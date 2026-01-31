package cash.ice.api.task;

import liquibase.database.Database;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Slf4j
public class PaygoDbMigrationTask extends DbMigrationTask {
    private static final String COLLECTION_NAME = "PaygoMerchant";
    private static final String MERCHANT_JSON = """
            {
                "_id" : ObjectId("626105b844fc193b8e5560cd"),
                "merchant" : {
                    "_id" : "6c464e00-1b49-4051-b222-5786ea60efba",
                    "created" : "2022-04-12T22:25:05.899031+02:00",
                    "updated" : "2022-04-21T09:39:08.50771+02:00",
                    "transactionCode" : "PGCBZ",
                    "name" : "ICEcash",
                    "countryCode" : "ZWE",
                    "city" : "Harare",
                    "region" : "Mashonaland",
                    "addressLine1" : "shop 1",
                    "addressLine2" : "rainy lane2",
                    "description" : "merchant desc",
                    "emailAddress" : "info@icecash.org",
                    "phoneNumber" : "+263773123124",
                    "url" : "http://icecash.test.org",
                    "mspReference" : "626105b844fc193b8e5560cd",
                    "active" : true
                },
                "credentials" : [
                    {
                        "_id" : "df1c1eac-5af9-4e1e-93b0-b6caddf0d945",
                        "merchantCredentialId" : "c0bb5024-01dc-4093-89be-7a639e111d9a",
                        "created" : "2022-04-21T09:27:41.181582+02:00",
                        "updated" : "2022-04-21T09:27:41.181581+02:00",
                        "merchantId" : "6c464e00-1b49-4051-b222-5786ea60efba",
                        "type" : "ACCEPTOR",
                        "currencyCode" : "ZWL",
                        "credential" : "123456",
                        "credentialReference" : "83d33750-553f-490c-94ec-647d39ee6b22",
                        "terminalId" : "12345678",
                        "cardAcceptorId" : "123456789012345",
                        "fiId" : "6737eb16-e86d-4457-ac5d-1f26f0265a58",
                        "active" : true
                    }
                ],
                "_class" : "cash.ice.paygo.entity.PaygoMerchant"
            }
                                """;

    @Override
    public void execute(Database database) {
        log.debug("> Migrating PayGO DB");
        try {
            MongoTemplate mongoTemplate = applicationContext.getBean(MONGO_TEMPLATE, MongoTemplate.class);
            if (mongoTemplate.count(query(where("_id").is("626105b844fc193b8e5560cd")), COLLECTION_NAME) == 0) {
                mongoTemplate.save(MERCHANT_JSON, COLLECTION_NAME);
            }
        } catch (Throwable e) {
            log.error(getClass().getSimpleName() + " failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "> Migrating PayGO DB finished. Committed.";
    }
}
