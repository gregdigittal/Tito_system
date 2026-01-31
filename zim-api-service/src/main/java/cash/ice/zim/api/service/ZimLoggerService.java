package cash.ice.zim.api.service;

import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.zim.api.dto.PaymentResponseZim;
import cash.ice.zim.api.dto.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@Slf4j
public class ZimLoggerService {
    private static final String ID = "_id";

    private final MongoTemplate mongoTemplate;
    private final String requestCollection;
    private final String responseCollection;

    public ZimLoggerService(MongoTemplate mongoTemplate,
                            @Value("${ice.cash.zim.mongodb.request-collection}") String requestCollection,
                            @Value("${ice.cash.zim.mongodb.response-collection}") String responseCollection) {
        this.mongoTemplate = mongoTemplate;
        this.requestCollection = requestCollection;
        this.responseCollection = responseCollection;
    }

    public boolean isRequestExist(String vendorRef) {
        Query query = query(where(ID).is(vendorRef));
        return mongoTemplate.exists(query, requestCollection);
    }

    public boolean isResponseExist(String vendorRef) {
        Query query = query(where(ID).is(vendorRef));
        return mongoTemplate.exists(query, responseCollection);
    }

    public <T> T getRequest(String vendorRef, Class<T> responseClass) {
        Query query = query(where(ID).is(vendorRef));
        return mongoTemplate.findOne(query, responseClass, requestCollection);
    }

    public <T> T getResponse(String vendorRef, Class<T> responseClass) {
        Query query = query(where(ID).is(vendorRef));
        return mongoTemplate.findOne(query, responseClass, responseCollection);
    }

    public <T> T getResponseByExternalTransactionId(String externalTransactionId, Class<T> responseClass) {
        Query query = query(where("externalTransactionId").is(externalTransactionId));
        return mongoTemplate.findOne(query, responseClass, responseCollection);
    }

    public boolean savePaymentRequest(String vendorRef, PaymentRequestZim request) {
        Objects.requireNonNull(vendorRef, "vendorRef must not be null!");
        Query query = query(where(ID).is(vendorRef));
        if (!mongoTemplate.exists(query, requestCollection)) {
            mongoTemplate.save(request, requestCollection);
            return true;
        } else {
            log.info("  Duplicated request vendorRef: {}, skipping payment: {}", vendorRef, request);
            return false;
        }
    }

    public void savePaymentResponse(String vendorRef, PaymentResponseZim response) {
        Objects.requireNonNull(vendorRef, "vendorRef must not be null!");
        mongoTemplate.save(response, responseCollection);
    }

    public void removePayment(String vendorRef) {
        Objects.requireNonNull(vendorRef, "vendorRef must not be null!");
        Query query = query(where(ID).is(vendorRef));
        mongoTemplate.remove(query, PaymentRequestZim.class, requestCollection);
        mongoTemplate.remove(query, PaymentResponseZim.class, responseCollection);
    }

    public <T> List<T> getResponsesBy(ResponseStatus status, Instant timeFrom, Instant timeTo, String errorCode, String message, String externalTransactionId,
                                      String spName, Integer spResult, String spMessage, String spError, Integer spTransactionId, Class<T> responseClass) {
        Criteria criteria = getResponsesCriteria(status, timeFrom, timeTo, errorCode, message, externalTransactionId, spName, spResult, spMessage, spError, spTransactionId);
        return mongoTemplate.find(query(criteria), responseClass, responseCollection);
    }

    public <T> long getResponsesCount(ResponseStatus status, Instant timeFrom, Instant timeTo, String errorCode, String message, String externalTransactionId,
                                      String spName, Integer spResult, String spMessage, String spError, Integer spTransactionId, Class<T> responseClass) {
        Criteria criteria = getResponsesCriteria(status, timeFrom, timeTo, errorCode, message, externalTransactionId, spName, spResult, spMessage, spError, spTransactionId);
        return mongoTemplate.count(query(criteria), responseClass, responseCollection);
    }

    private Criteria getResponsesCriteria(ResponseStatus status, Instant timeFrom, Instant timeTo, String errorCode, String message, String externalTransactionId, String spName, Integer spResult, String spMessage, String spError, Integer spTransactionId) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = new ArrayList<>();
        if (status != null) {
            criteriaList.add(where("status").is(status));
        }
        if (timeFrom != null) {
            criteriaList.add(where("date").gte(timeFrom));
        }
        if (timeTo != null) {
            criteriaList.add(where("date").lte(timeTo));
        }
        if (errorCode != null) {
            criteriaList.add(where("errorCode").is(errorCode.equals("null") ? null : errorCode));
        }
        if (message != null) {
            criteriaList.add(message.equals("null") ? where("message").is(null) : where("message").regex(".*" + message + ".*", "i"));
        }
        if (externalTransactionId != null) {
            criteriaList.add(where("externalTransactionId").is(externalTransactionId.equals("null") ? null : externalTransactionId));
        }
        if (spName != null) {
            criteriaList.add(where("spResult.spName").is(spName.equals("null") ? null : spName));
        }
        if (spResult != null) {
            criteriaList.add(where("spResult.result").is(spResult));
        }
        if (spMessage != null) {
            criteriaList.add(spMessage.equals("null") ? where("spResult.message").is(null) : where("spResult.message").regex(".*" + spMessage + ".*", "i"));
        }
        if (spError != null) {
            criteriaList.add(spError.equals("null") ? where("spResult.error").is(null) : where("spResult.error").regex(".*" + spError + ".*", "i"));
        }
        if (spTransactionId != null) {
            criteriaList.add(where("spResult.transactionId").is(spTransactionId));
        }
        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        return criteria;
    }
}
