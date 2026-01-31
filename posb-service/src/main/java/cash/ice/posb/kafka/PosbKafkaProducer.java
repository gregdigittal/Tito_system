package cash.ice.posb.kafka;

import cash.ice.posb.service.impl.PosbPaymentServiceImpl;
import cash.ice.common.dto.zim.PaymentErrorZim;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.messaging.annotation.MessageHeader;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static cash.ice.posb.service.impl.PosbPaymentServiceImpl.TYPE_ID_HEADER;

@KafkaClient(id = "${ice.cash.kafka.client}")
public interface PosbKafkaProducer {

    @Topic("${ice.cash.kafka.response-topic}")
    void sendPaymentResponse(@KafkaKey String id, Object response, Collection<Header> headers);

    @Topic("${ice.cash.kafka.error-topic}")
    void sendPaymentError(@KafkaKey String id, PaymentErrorZim errorData, @MessageHeader(TYPE_ID_HEADER) String typeIdHeader);

    default void sendPaymentResponse(String id, Object response, boolean addServiceHeader) {
        List<Header> headers = new ArrayList<>(List.of(new RecordHeader(TYPE_ID_HEADER, response.getClass().getCanonicalName().getBytes())));
        if (addServiceHeader) {
            headers.add(new RecordHeader(PosbPaymentServiceImpl.SERVICE_HEADER, "".getBytes()));
        }
        sendPaymentResponse(id, response, headers);
    }

    default void sendPaymentError(String id, PaymentErrorZim errorData) {
        sendPaymentError(id, errorData, errorData.getClass().getCanonicalName());
    }
}
