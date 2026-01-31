package cash.ice.ecocash.controller;

import cash.ice.ecocash.config.EcocashProperties;
import cash.ice.ecocash.dto.ReversalStatus;
import cash.ice.ecocash.entity.EcocashPayment;
import cash.ice.ecocash.service.EcocashPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1"})
@RequiredArgsConstructor
@Slf4j
public class EcocashController {
    private final EcocashPaymentService ecocashPaymentService;
    private final EcocashProperties ecocashProperties;

    @PostMapping("/transaction/refund/{vendorRefOrTransactionId}")
    public ReversalStatus performManualRefund(@PathVariable String vendorRefOrTransactionId) {
        log.info("> manual refund: {}", vendorRefOrTransactionId);
        return ecocashPaymentService.manualRefund(vendorRefOrTransactionId);
    }

    @GetMapping("/debug/{vendorRef}")
    public ResponseEntity<EcocashPayment> getDebugInfo(@PathVariable String vendorRef) {
        log.info("> GET debug info for: {}", vendorRef);
        EcocashPayment ecocashPayment = ecocashPaymentService.getEcocashPayment(vendorRef);
        return new ResponseEntity<>(ecocashPayment, ecocashPayment != null ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @GetMapping("/debug/props")
    public EcocashProperties getDebugPropsInfo() {
        log.info("> GET debug props info");
        return ecocashProperties;
    }
}