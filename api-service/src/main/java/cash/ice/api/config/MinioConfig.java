package cash.ice.api.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioConfig {
    private String url;
    private String accessName;
    private String accessSecret;

    @Bean
    public MinioClient generateMinioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessName, accessSecret)
                .build();
    }
}
