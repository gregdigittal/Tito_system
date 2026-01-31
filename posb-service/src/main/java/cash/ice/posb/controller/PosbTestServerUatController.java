package cash.ice.posb.controller;

import cash.ice.posb.dto.posb.*;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller("/posb")
@RequiredArgsConstructor
@Requires(notEnv = "prod-k8s")
@Slf4j
public class PosbTestServerUatController {

    @Post("/api/v1/payment/instruction")
    public HttpResponse<?> posbInstruction(@NonNull @Body PosbInstructionRequest request, HttpHeaders headers) {
        String xApiKey = headers.get("X-API-Key");
        String xTraceId = headers.get("X-Trace-ID");
        log.info("|> posb instruction: {}, apiKey: {}, traceId: {}", request, xApiKey, xTraceId);
        if (xTraceId.equals("instructionError")) {
            return HttpResponse.serverError(
                    new PosbInstructionResponse()
                            .setCode(500)
                            .setMessage("Simulated instruction error"));
        } else {
            return HttpResponse.created(
                    new PosbInstructionResponse()
                            .setPaymentReference(request.getPaymentReference())
                            .setIcecashPoolAccountNumber(request.getCustomerAccountNumber())
                            .setCurrency(request.getCurrency())
                            .setCustomerName("EMMANUEL MANESWA")
                            .setCustomerMobileNumber("263788786951")
                            .setStatus("PENDING")
                            .setNarrative("OTP sent to customer mobile number")
                            .setOtpExpiringTime("2023-06-21T12:41:35.7965481+02:00")
            );
        }
    }

    @Post("/api/v1/payment/confirmition")
    public HttpResponse<?> posbConfirmation(@NonNull @Body PosbConfirmationRequest request) {
        log.info("|> posb confirmation: " + request);
        return HttpResponse.ok(
                new PosbConfirmationResponse()
                        .setPaymentReference(request.getPaymentReference())
                        .setStatus("SUCCESSFUL")
                        .setResponseCode(0)
                        .setNarrative("Success")
        );
    }

    @Get("/api/v1/payment/status/{paymentReference}")
    public HttpResponse<?> posbGetStatus(String paymentReference) {
        log.info("|> posb status: " + paymentReference);
        return HttpResponse.ok(
                new PosbStatusResponse()
                        .setPaymentReference(paymentReference)
                        .setStatus("SUCCESSFUL")
                        .setReversed(false)
                        .setReversalReference(null)
                        .setResponseCode(0)
                        .setNarrative("Success")
        );
    }

    @Post("/api/v1/payment/reversal")
    public HttpResponse<?> posbReversal(@NonNull @Body PosbReversalRequest request) {
        log.info("|> posb reversal: " + request);
        return HttpResponse.ok(
                new PosbReversalResponse()
                        .setOriginalPaymentReference(request.getOriginalPaymentReference())
                        .setPaymentReversalReference(request.getPaymentReversalReference())
                        .setReversedDateTime("2023-06-22T11:31:51.5836187+02:00")
                        .setReversalStatus("SUCCESSFUL")
                        .setResponseCode(0)
                        .setNarrative("Success")
        );
    }
}