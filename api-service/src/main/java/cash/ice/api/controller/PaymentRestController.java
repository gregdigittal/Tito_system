package cash.ice.api.controller;

import cash.ice.api.service.PaymentService;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
@RequestMapping({"/api/v1/payments", "/api/v1/unsecure/payments"})
@RequiredArgsConstructor
@Slf4j
public class PaymentRestController {
    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(code = ACCEPTED)
    public void addPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        paymentService.addPayment(paymentRequest);
    }

    @GetMapping("/{vendorRef}")
    public PaymentRequest getPaymentRequest(@PathVariable String vendorRef) {
        return paymentService.getPaymentRequest(vendorRef);
    }

    @GetMapping("/{vendorRef}/response")
    public PaymentResponse getPaymentResponse(@PathVariable String vendorRef) {
        return paymentService.getPaymentResponse(vendorRef);
    }

    @PostMapping("/synchronous")
    @ResponseStatus(code = ACCEPTED)
    public PaymentResponse addPaymentSynchronous(@Valid @RequestBody PaymentRequest paymentRequest) {
        return paymentService.makePaymentSynchronous(paymentRequest);
    }
}