package cash.ice.api.config;

import cash.ice.api.graphql.scalar.DateTimeScalar;
import cash.ice.api.graphql.scalar.MapScalar;
import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQlConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.LocalTime)
                .scalar(DateTimeScalar.INSTANCE)
                .scalar(MapScalar.INSTANCE);
    }
}
