package cash.ice.api.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioConfig {
    private String url;
    private String accessName;
    private String accessSecret;

    /** Only create MinioClient when minio.url is set (e.g. for Tito deploy without document storage, omit MINIO_* vars). */
    @Bean
    @ConditionalOnProperty(prefix = "minio", name = "url")
    public MinioClient generateMinioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessName, accessSecret)
                .build();
    }
}
