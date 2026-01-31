package cash.ice.api.service;

import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface LoggerService {

    boolean isRequestExist(String vendorRef);

    boolean isResponseExist(String vendorRef);

    <T> T getRequest(String vendorRef, Class<T> responseClass);

    <T> List<T> getRequests(List<String> vendorRefs, Class<T> responseClass);

    <T> T getResponse(String vendorRef, Class<T> responseClass);

    PaymentResponse waitForResponse(String vendorRef, Instant startTime, Duration maxWaitDuration);

    boolean savePaymentRequest(String vendorRef, PaymentRequest request);

    void savePaymentResponse(String vendorRef, PaymentResponse response);

    void removePayment(String vendorRef);
}
