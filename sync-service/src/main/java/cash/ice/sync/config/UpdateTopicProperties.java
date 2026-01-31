package cash.ice.sync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ice.cash.update.topic")
@Component
@Data
public class UpdateTopicProperties {
    private String entity;
    private String account;
    private String accountRelationship;
    private String securityGroup;
    private String securityRight;
    private String initiator;
    private String channel;
    private String taxDeclaration;
    private String taxReason;
    private String paymentMethod;
    private String bank;
    private String currency;
    private String wallet;
    private String entityTypeGroup;
    private String entityType;
    private String initiatorCategory;
    private String initiatorStatus;
    private String documentType;
}
