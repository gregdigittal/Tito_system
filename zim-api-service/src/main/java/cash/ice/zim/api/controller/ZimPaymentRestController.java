package cash.ice.zim.api.controller;

import cash.ice.common.dto.zim.PaymentOtpRequestZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.utils.Tool;
import cash.ice.zim.api.aop.ApiKeyRestricted;
import cash.ice.zim.api.config.ZimApiProperties;
import cash.ice.zim.api.dto.PaymentResponseZim;
import cash.ice.zim.api.error.NotFoundException;
import cash.ice.zim.api.service.ZimLoggerService;
import cash.ice.zim.api.service.ZimPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
@RequestMapping("/api/v1/zim/payment")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ZimPaymentRestController {
    private final ZimPaymentService paymentService;
    private final ZimLoggerService loggerService;
    private final ZimApiProperties zimApiProperties;
    private final RestTemplate restTemplate;

    @ApiKeyRestricted
    @PostMapping
    @ResponseStatus(code = ACCEPTED)
    public PaymentResponseZim addPayment(@Valid @RequestBody PaymentRequestZim request) {
        log.debug("> payment: " + request);
        paymentService.addNewPayment(request);
        return new PaymentResponseZim().setVendorRef(request.getVendorRef())
                .setStatus(cash.ice.zim.api.dto.ResponseStatus.PROCESSING).setDate(Tool.currentDateTime());
    }

    @ApiKeyRestricted
    @PostMapping("/sync")
    @ResponseStatus(code = ACCEPTED)
    public PaymentResponseZim makePaymentSync(@Valid @RequestBody PaymentRequestZim request) {
        log.debug("> payment (sync): " + request);
        return paymentService.makePaymentSync(request);
    }

    @ApiKeyRestricted
    @PostMapping("/otp")
    public PaymentResponseZim handlePaymentOtp(@Valid @RequestBody PaymentOtpRequestZim request) {
        log.debug("> payment otp: " + request);
        return paymentService.handlePaymentOtp(request);
    }

    @ApiKeyRestricted
    @PostMapping("/otp/sync")
    @ResponseStatus(code = ACCEPTED)
    public PaymentResponseZim handlePaymentOtpSync(@Valid @RequestBody PaymentOtpRequestZim request) {
        log.debug("> payment otp (sync): " + request);
        return paymentService.handlePaymentOtpSync(request);
    }

    @ApiKeyRestricted
    @GetMapping("/{vendorRef}/request")
    public PaymentRequestZim getPaymentRequest(@PathVariable String vendorRef) {
        log.debug("> get payment request: " + vendorRef);
        PaymentRequestZim request = paymentService.getPaymentRequest(vendorRef);
        if (request == null) {
            throw new NotFoundException(String.format("PaymentRequest with vendorRef: '%s' does not exist", vendorRef), ErrorCodes.EC1103);
        }
        return request;
    }

    @ApiKeyRestricted
    @GetMapping("/{vendorRef}/response")
    public PaymentResponseZim getPaymentResponse(@PathVariable String vendorRef) {
        log.debug("> get payment response: " + vendorRef);
        PaymentResponseZim response = paymentService.getPaymentResponse(vendorRef);
        if (response == null) {
            throw new NotFoundException(String.format("PaymentResponse with vendorRef: '%s' does not exist", vendorRef), ErrorCodes.EC1103);
        }
        return response;
    }

    @ApiKeyRestricted
    @GetMapping("/{service}/{vendorRef}/status")
    public Map<?, ?> getTransactionStatus(@PathVariable String service, @PathVariable String vendorRef) {
        String url = String.format(zimApiProperties.getStatusUrl(), zimApiProperties.getServiceHost().get(service), vendorRef);
        log.debug("> get {} transaction status: {}, url: {}", service, vendorRef, url);
        return restTemplate.getForObject(url, Map.class);
    }

    @ApiKeyRestricted(internal = true)
    @PostMapping("/{service}/{vendorRef}/reversal")
    public Map<?, ?> performManualRefund(@PathVariable String service, @PathVariable String vendorRef,
                                         @RequestParam(value = "useExternalTransactionId", required = false) Boolean useExternalTransactionId) {
        log.debug("> manual {} refund: {}, useExternalTransactionId param: {}", service, vendorRef, useExternalTransactionId);
        return paymentService.performManualRefund(service, vendorRef, useExternalTransactionId);
    }

    @ApiKeyRestricted(internal = true)
    @PutMapping("/{vendorRef}/response")
    public PaymentResponseZim getPaymentResponse(@PathVariable String vendorRef, @RequestParam(value = "status") String status) {
        log.debug("> set {} payment response status, vendorRef: {}", status, vendorRef);
        PaymentResponseZim response = paymentService.getPaymentResponse(vendorRef);
        if (response == null) {
            throw new NotFoundException(String.format("PaymentResponse with vendorRef: '%s' does not exist", vendorRef), ErrorCodes.EC1103);
        }
        paymentService.updateStatus(response, status);
        return response;
    }

    @ApiKeyRestricted
    @GetMapping("/{service}/{msisdn}/name")
    public Map<?, ?> getPaymentAccountName(@PathVariable String service, @PathVariable String msisdn) {
        String url = String.format(zimApiProperties.getNameInfoUrl(), zimApiProperties.getServiceHost().get(service), msisdn);
        log.debug("> get {} msisdn name: {}, url: {}", service, msisdn, url);
        return restTemplate.getForObject(url, Map.class);
    }

    @ApiKeyRestricted(internal = true)
    @GetMapping("/{service}/{vendorRef}/debug")
    public Map<?, ?> getServicePaymentDebugInfo(@PathVariable String service, @PathVariable String vendorRef,
                                                @RequestParam(value = "addScripts", required = false) Boolean addScripts, @RequestParam(value = "addProps", required = false) Boolean addProps) {
        String url = String.format(zimApiProperties.getDebugInfoUrl(), zimApiProperties.getServiceHost().get(service), vendorRef);
        log.debug("> get {} debug info for: {}, url: {}, addScripts: {}, addProps: {}", service, vendorRef, url, addScripts, addProps);
        return paymentService.getServicePaymentDebugInfo(service, vendorRef, addScripts, addProps);
    }

    @GetMapping("/{service}/actuator/health")
    public Map<?, ?> getServiceActuatorHealth(@PathVariable String service) {
        String url = String.format(zimApiProperties.getActuatorHealthUrl(), zimApiProperties.getServiceHost().get(service));
        log.debug("> get {} actuator health, url: {}", service, url);
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.info("{}: '{}' occurred while getting actuator health for {}", e.getClass().getCanonicalName(), e.getMessage(), service);
            return new LinkedHashMap<>(Map.of("status", "ERROR", "message", e.getMessage(), "date", Tool.currentDateTime()));
        }
    }

    @ApiKeyRestricted(internal = true)
    @GetMapping("/responses")
    public Object getResponsesStatistics(@RequestParam(value = "status", required = false) cash.ice.zim.api.dto.ResponseStatus status,
                                         @RequestParam(value = "timeFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant timeFrom,
                                         @RequestParam(value = "timeTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant timeTo,
                                         @RequestParam(value = "errorCode", required = false) String errorCode,
                                         @RequestParam(value = "message", required = false) String message,
                                         @RequestParam(value = "externalTransactionId", required = false) String externalTransactionId,
                                         @RequestParam(value = "spName", required = false) String spName,
                                         @RequestParam(value = "spResult", required = false) Integer spResult,
                                         @RequestParam(value = "spMessage", required = false) String spMessage,
                                         @RequestParam(value = "spError", required = false) String spError,
                                         @RequestParam(value = "spTransactionId", required = false) Integer spTransactionId,
                                         @RequestParam(value = "returnCount", required = false) Boolean returnCount) {
        log.debug("> get payment responses. status: {}, timeFrom: {}, timeTo: {}, errorCode: {}, message: {}, externalTransactionId: {}, spName: {}, spResult: {}, spMessage: {}, spError: {}, spTransactionId: {}, returnCount: {}",
                status, timeFrom, timeTo, errorCode, message, externalTransactionId, spName, spResult, spMessage, spError, spTransactionId, returnCount);
        if (returnCount == Boolean.TRUE) {
            return Map.of("count", loggerService.getResponsesCount(status, timeFrom, timeTo, errorCode, message, externalTransactionId, spName, spResult, spMessage, spError, spTransactionId, PaymentResponseZim.class));
        } else {
            return loggerService.getResponsesBy(status, timeFrom, timeTo, errorCode, message, externalTransactionId, spName, spResult, spMessage, spError, spTransactionId, PaymentResponseZim.class);
        }
    }
}
