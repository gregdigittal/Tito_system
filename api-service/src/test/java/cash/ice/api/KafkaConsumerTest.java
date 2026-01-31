package cash.ice.api;

import cash.ice.common.dto.PaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Not a test. Used locally to consume data from a kafka topic")
class KafkaConsumerTest {
    private static final String TOPIC = "ice.cash.payment.RequestTopic";

    @Test
    void consume() {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:29092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "KafkaTestConsumer");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ObjectMapper objectMapper = new ObjectMapper();
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(List.of(TOPIC));
        assertThat(consumer.listTopics().size()).isPositive();

        while (Thread.currentThread().isAlive()) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                System.out.println(">>>>> Key: " + record.key() + ", Value: " + record.value() + ", Partition: " +
                        record.partition() + ", Offset: " + record.offset());
                try {
                    PaymentRequest paymentRequest = objectMapper.readValue(record.value(), PaymentRequest.class);
                    System.out.println(">>>>> request: " + paymentRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
