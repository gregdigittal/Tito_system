package cash.ice.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class NewDbConfiguration {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource newDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.legacy-datasource")
    public DataSource legacyDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.legacy-config-datasource")
    public DataSource legacyConfigDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(legacyDataSource());
    }

    @Bean
    public JdbcTemplate configJdbcTemplate() {
        return new JdbcTemplate(legacyConfigDataSource());
    }

    @Bean
    public JdbcTemplate newJdbcTemplate() {
        return new JdbcTemplate(newDataSource());
    }
}
