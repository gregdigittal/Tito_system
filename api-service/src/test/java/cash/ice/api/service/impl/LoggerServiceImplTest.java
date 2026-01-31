package cash.ice.api.service.impl;

import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@ExtendWith(MockitoExtension.class)
class LoggerServiceImplTest {
    public static final String VENDOR_REF = "testVendor";
    private static final String REQUEST_COLLECTION = "requestCollection";
    private static final String RESPONSE_COLLECTION = "responseCollection";

    @Mock
    private MongoTemplate mongoTemplate;
    private LoggerServiceImpl service;

    @BeforeEach
    void init() {
        service = new LoggerServiceImpl(mongoTemplate, REQUEST_COLLECTION, RESPONSE_COLLECTION);
    }

    @Test
    void getResponse() {
        Class<String> clazz = String.class;
        String expectedResult = "result";

        doReturn(expectedResult).when(mongoTemplate).findOne(query(where("_id").is(VENDOR_REF)), clazz, RESPONSE_COLLECTION);
        String actualResponse = service.getResponse(VENDOR_REF, clazz);

        assertThat(actualResponse).isEqualTo(expectedResult);
    }

    @Test
    void testSavePaymentRequest() {
        PaymentRequest testRequest = new PaymentRequest();

        doReturn(false).when(mongoTemplate).exists(query(where("_id").is(VENDOR_REF)), REQUEST_COLLECTION);
        boolean saved = service.savePaymentRequest(VENDOR_REF, testRequest);

        assertThat(saved).isTrue();
        verify(mongoTemplate).save(testRequest, REQUEST_COLLECTION);
    }

    @Test
    void testSavePaymentRequestDuplicate() {
        PaymentRequest testRequest = new PaymentRequest();

        doReturn(true).when(mongoTemplate).exists(query(where("_id").is(VENDOR_REF)), REQUEST_COLLECTION);
        boolean saved = service.savePaymentRequest(VENDOR_REF, testRequest);

        assertThat(saved).isFalse();
        verify(mongoTemplate, never()).save(any(), any());
    }

    @Test
    void testSavePaymentResponse() {
        PaymentResponse testResponse = PaymentResponse.success(VENDOR_REF, "1", BigDecimal.ONE, null, null);

        when(mongoTemplate.findOne(query(where("_id").is(VENDOR_REF)), PaymentResponse.class, RESPONSE_COLLECTION))
                .thenReturn(null);
        service.savePaymentResponse(VENDOR_REF, testResponse);

        verify(mongoTemplate).save(testResponse, RESPONSE_COLLECTION);
    }
}