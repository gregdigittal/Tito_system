package cash.ice.common.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicsConfig {
    private final KafkaTopicsProperties topics;

    @Bean
    public NewTopic paymentRequestTopic() {
        return TopicBuilder.name(topics.getPaymentRequest())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic feeServiceTopic() {
        return TopicBuilder.name(topics.getFeeService())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic paymentSelectorTopic() {
        return TopicBuilder.name(topics.getPaymentSelector())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic subPaymentResultTopic() {
        return TopicBuilder.name(topics.getSubPaymentResult())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic paygoServiceTopic() {
        return TopicBuilder.name(topics.getPaygoService())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic paygoSuccessPaymentTopic() {
        return TopicBuilder.name(topics.getPaygoSuccessPayment())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic ecocashServiceTopic() {
        return TopicBuilder.name(topics.getEcocashService())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic ecocashSuccessPaymentTopic() {
        return TopicBuilder.name(topics.getEcocashSuccessPayment())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic onemoneyServiceTopic() {
        return TopicBuilder.name(topics.getOnemoneyService())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic onemoneySuccessPaymentTopic() {
        return TopicBuilder.name(topics.getOnemoneySuccessPayment())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic zimSwitchServiceTopic() {
        return TopicBuilder.name(topics.getZimSwitchService())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic zimSwitchSuccessPaymentTopic() {
        return TopicBuilder.name(topics.getZimSwitchSuccessPayment())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic mpesaServiceTopic() {
        return TopicBuilder.name(topics.getMpesaService())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic mpesaSuccessPaymentTopic() {
        return TopicBuilder.name(topics.getMpesaSuccessPayment())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic emolaServiceTopic() {
        return TopicBuilder.name(topics.getEmolaService())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic emolaSuccessPaymentTopic() {
        return TopicBuilder.name(topics.getEmolaSuccessPayment())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic zimPaymentRequestTopic() {
        return TopicBuilder.name(topics.getZimPaymentRequest())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic zimPaymentResultTopic() {
        return TopicBuilder.name(topics.getZimPaymentResult())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic zimPaymentErrorTopic() {
        return TopicBuilder.name(topics.getZimPaymentError())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic posbServiceTopic() {
        return TopicBuilder.name(topics.getPosbService())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic fbcServiceTopic() {
        return TopicBuilder.name(topics.getFbcService())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic fcbServiceTopic() {
        return TopicBuilder.name(topics.getFcbService())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic ledgerServiceTopic() {
        return TopicBuilder.name(topics.getLedgerService())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic successPaymentTopic() {
        return TopicBuilder.name(topics.getSuccessPayment())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic errorPaymentTopic() {
        return TopicBuilder.name(topics.getErrorPayment())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }

    @Bean
    public NewTopic afterSuccessPaymentTopic() {
        return TopicBuilder.name(topics.getAfterSuccessPayment())
                .partitions(topics.getPartitions()).replicas(topics.getReplicas()).build();
    }
}
