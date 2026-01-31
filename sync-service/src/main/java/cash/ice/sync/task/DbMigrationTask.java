package cash.ice.sync.task;

import cash.ice.sync.service.DataMigrator;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;

@Slf4j
@Setter
public class DbMigrationTask implements CustomTaskChange {
    protected ApplicationContext applicationContext;
    protected String serviceName;

    public void execute(Database database) {
        log.debug("> Migrating {}", serviceName);
        try {
            DataMigrator dataMigrator = applicationContext.getBean(serviceName, DataMigrator.class);
            dataMigrator.migrateData();
        } catch (Throwable e) {
            log.error("DbMigrationTask failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "> Migrating " + serviceName + " finished. Committed.";
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        try {
            Field resourceLoaderField = resourceAccessor.getClass().getDeclaredField("resourceLoader");
            resourceLoaderField.setAccessible(true);
            applicationContext = (ApplicationContext) resourceLoaderField.get(resourceAccessor);
            applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(
                    this, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setUp() {
        // no use
    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }
}
