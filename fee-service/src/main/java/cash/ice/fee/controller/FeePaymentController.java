package cash.ice.fee.controller;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.service.KafkaSender;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
@RequestMapping({"/api/v1/payments", "/api/v1/unsecure/payments"})
@RequiredArgsConstructor
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class FeePaymentController {
    private final KafkaSender kafkaSender;

    @PostMapping
    @ResponseStatus(code = ACCEPTED)
    public void addApiPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        log.debug("> " + paymentRequest);
        kafkaSender.sendFeeService(paymentRequest.getVendorRef(), paymentRequest);
    }
}
