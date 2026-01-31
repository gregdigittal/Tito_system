package cash.ice.api.service.impl;

import cash.ice.api.service.LoggerService;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.dto.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static cash.ice.common.utils.Tool.sleep;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@Slf4j
public class LoggerServiceImpl implements LoggerService {
    private static final String ID = "_id";

    private final MongoTemplate mongoTemplate;
    private final String requestCollection;
    private final String responseCollection;

    public LoggerServiceImpl(MongoTemplate mongoTemplate,
                             @Value("${ice.cash.mongodb.request-collection}") String requestCollection,
                             @Value("${ice.cash.mongodb.response-collection}") String responseCollection) {
        this.mongoTemplate = mongoTemplate;
        this.requestCollection = requestCollection;
        this.responseCollection = responseCollection;
    }

    @Override
    public boolean isRequestExist(String vendorRef) {
        Query query = query(where(ID).is(vendorRef));
        return mongoTemplate.exists(query, requestCollection);
    }

    @Override
    public boolean isResponseExist(String vendorRef) {
        Query query = query(where(ID).is(vendorRef));
        return mongoTemplate.exists(query, responseCollection);
    }

    @Override
    public <T> T getRequest(String vendorRef, Class<T> responseClass) {
        Query query = query(where(ID).is(vendorRef));
        return mongoTemplate.findOne(query, responseClass, requestCollection);
    }

    @Override
    public <T> List<T> getRequests(List<String> vendorRefs, Class<T> responseClass) {
        Query query = query(where(ID).in(vendorRefs));
        return mongoTemplate.find(query, responseClass, requestCollection);
    }

    @Override
    public <T> T getResponse(String vendorRef, Class<T> responseClass) {
        Query query = query(where(ID).is(vendorRef));
        return mongoTemplate.findOne(query, responseClass, responseCollection);
    }

    @Override
    public PaymentResponse waitForResponse(String vendorRef, Instant startTime, Duration maxWaitDuration) {
        while (Instant.now().isBefore(startTime.plus(maxWaitDuration))) {
            PaymentResponse response = getResponse(vendorRef, PaymentResponse.class);
            if (response != null && response.getStatus() != ResponseStatus.PROCESSING) {
                return response;
            }
            sleep(100);
        }
        return null;
    }

    @Override
    public boolean savePaymentRequest(String vendorRef, PaymentRequest request) {
        Objects.requireNonNull(vendorRef, "vendorRef must not be null!");
        Query query = query(where(ID).is(vendorRef));
        if (!mongoTemplate.exists(query, requestCollection)) {
            mongoTemplate.save(request, requestCollection);
            return true;
        } else {
            log.info("Duplicated request vendorRef: {}, skipping payment: {}", vendorRef, request);
            return false;
        }
    }

    @Override
    public void savePaymentResponse(String vendorRef, PaymentResponse response) {
        Objects.requireNonNull(vendorRef, "vendorRef must not be null!");
        PaymentResponse existingResponse = getResponse(vendorRef, PaymentResponse.class);
        if (existingResponse != null && existingResponse.getPayload() != null) {
            if (response.getPayload() == null) {
                response.setPayload(new HashMap<>());
            }
            existingResponse.getPayload().forEach((k, v) -> response.getPayload().putIfAbsent(k, v));
        }
        mongoTemplate.save(response, responseCollection);
    }

    @Override
    public void removePayment(String vendorRef) {
        Objects.requireNonNull(vendorRef, "vendorRef must not be null!");
        Query query = query(where(ID).is(vendorRef));
        mongoTemplate.remove(query, PaymentRequest.class, requestCollection);
        mongoTemplate.remove(query, PaymentResponse.class, responseCollection);
    }
}
