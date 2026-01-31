package cash.ice.api.task;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.Setter;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;

@Setter
abstract class DbMigrationTask implements CustomTaskChange {
    protected static final String MONGO_TEMPLATE = "mongoTemplate";
    protected ApplicationContext applicationContext;

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        try {
            Class<? extends ResourceAccessor> resourceAccessorClass = resourceAccessor.getClass();
            Field resourceLoaderField = resourceAccessorClass.getDeclaredField("resourceLoader");
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
