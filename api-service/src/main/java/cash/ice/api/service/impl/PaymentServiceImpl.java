package cash.ice.api.service.impl;

import cash.ice.api.errors.ApiPaymentException;
import cash.ice.api.service.LoggerService;
import cash.ice.api.service.PaymentService;
import cash.ice.api.service.TicketService;
import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.dto.ResponseStatus;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.service.KafkaSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final LoggerService loggerService;
    private final TicketService ticketService;
    private final KafkaSender kafkaSender;

    @Override
    public void addPayment(PaymentRequest paymentRequest) {
        log.debug("> " + paymentRequest);
        try {
            kafkaSender.sendPaymentRequest(paymentRequest.getVendorRef(), paymentRequest);
        } catch (Exception e) {
            throw new ApiPaymentException(paymentRequest.getVendorRef(), ErrorCodes.EC1002,
                    "Failed to send payment request to kafka topic", e);
        }
    }

    @Override
    public PaymentResponse makePaymentSynchronous(PaymentRequest paymentRequest) {
        return makePaymentSynchronous(paymentRequest, Duration.ofSeconds(60), (req, res) -> {
        });
    }

    @Override
    public PaymentResponse makePaymentSynchronous(PaymentRequest paymentRequest, Duration maxWaitDuration, BiConsumer<PaymentRequest, PaymentResponse> afterPaymentAction) {
        PaymentResponse response = loggerService.getResponse(paymentRequest.getVendorRef(), PaymentResponse.class);
        if (response != null) {
            log.warn("  already finished payment for vendorRef: {}", paymentRequest.getVendorRef());
            return response;
        }
        log.debug("  new payment: {}", paymentRequest.getVendorRef());
        kafkaSender.sendPaymentRequest(paymentRequest.getVendorRef(), paymentRequest);
        PaymentResponse paymentResponse = loggerService.waitForResponse(
                paymentRequest.getVendorRef(), Instant.now(), maxWaitDuration);
        if (paymentResponse != null) {
            afterPaymentAction.accept(paymentRequest, paymentResponse);
            return paymentResponse;
        } else {
            return PaymentResponse.error(paymentRequest.getVendorRef(), ErrorCodes.EC1031,                  // todo log error response
                    "Request timeout for vendorRef: " + paymentRequest.getVendorRef());
        }
    }

    @Override
    public List<PaymentResponse> makeBulkPaymentSynchronous(List<PaymentRequest> paymentRequestList, boolean ticketForFail,
                                                            Duration maxWaitDuration, BiConsumer<PaymentRequest, PaymentResponse> afterPaymentAction) {
        List<PaymentResponse> responses = new ArrayList<>();
        List<PaymentRequest> sentRequests = new ArrayList<>();
        paymentRequestList.forEach(paymentRequest -> {
            PaymentResponse response = loggerService.getResponse(paymentRequest.getVendorRef(), PaymentResponse.class);
            if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                log.warn("  already finished payment for vendorRef: {}", paymentRequest.getVendorRef());
                responses.add(response);
            } else {
                if (response != null) {
                    loggerService.removePayment(paymentRequest.getVendorRef());
                }
                paymentRequest.addMetaData(PaymentMetaKey.OffloadTransaction, true);
                kafkaSender.sendPaymentRequest(paymentRequest.getVendorRef(), paymentRequest);
                sentRequests.add(paymentRequest);
            }
        });
        Instant startWaitTime = Instant.now();
        sentRequests.forEach(paymentRequest -> {
            PaymentResponse paymentResponse = loggerService.waitForResponse(
                    paymentRequest.getVendorRef(), startWaitTime, maxWaitDuration);
            if (paymentResponse != null) {
                afterPaymentAction.accept(paymentRequest, paymentResponse);
                responses.add(paymentResponse);
            } else {
                responses.add(PaymentResponse.error(paymentRequest.getVendorRef(), ErrorCodes.EC1031,                  // todo log error response
                        "Request timeout for vendorRef: " + paymentRequest.getVendorRef()));
            }
        });
        List<PaymentResponse> sortedResponses = responses.stream().sorted(Comparator.comparing(PaymentResponse::getDate)).toList();
        if (ticketForFail) {
            ticketService.createTicketFor(sortedResponses.stream().filter(response -> response.getStatus() != ResponseStatus.SUCCESS).toList(), paymentRequestList);
        }
        return sortedResponses;
    }

    @Override
    public PaymentRequest getPaymentRequest(String vendorRef) {
        PaymentRequest paymentRequest = loggerService.getRequest(vendorRef, PaymentRequest.class);
        log.info("> get PaymentRequest (vendorRef={}): {}", vendorRef, paymentRequest);
        return paymentRequest;
    }

    @Override
    public PaymentResponse getPaymentResponse(String vendorRef) {
        PaymentResponse response = loggerService.getResponse(vendorRef, PaymentResponse.class);
        log.info("> get PaymentResponse (vendorRef={}): {}", vendorRef, response);
        if (response == null) {
            if (loggerService.isRequestExist(vendorRef)) {
                return PaymentResponse.processing(vendorRef);
            } else {
                return PaymentResponse.error(vendorRef, ErrorCodes.EC1005,
                        "Payment request was not found for vendorRef: " + vendorRef);
            }
        }
        return response;
    }
}
