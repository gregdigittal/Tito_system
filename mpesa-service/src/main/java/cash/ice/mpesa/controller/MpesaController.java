package cash.ice.mpesa.controller;

import cash.ice.common.dto.BeneficiaryNameResponse;
import cash.ice.mpesa.config.MpesaProperties;
import cash.ice.mpesa.dto.ReversalStatus;
import cash.ice.mpesa.dto.TransactionStatus;
import cash.ice.mpesa.entity.MpesaPayment;
import cash.ice.mpesa.service.MpesaPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/mpesa", "/api/v1"})
@RequiredArgsConstructor
@Slf4j
public class MpesaController {
    private final MpesaPaymentService mpesaPaymentService;
    private final MpesaProperties mpesaProperties;

    @GetMapping("/name/{msisdn}")
    public BeneficiaryNameResponse getMpesaNameByMsisdn(@PathVariable String msisdn) {
        log.info("> GET mpesa name for msisdn: " + msisdn);
        return mpesaPaymentService.queryCustomerName(msisdn);
    }

    @PostMapping("/transaction/refund/{vendorRefOrTransactionId}")
    public ReversalStatus performManualRefund(@PathVariable String vendorRefOrTransactionId) {
        log.info("> manual refund: {}", vendorRefOrTransactionId);
        return mpesaPaymentService.manualRefund(vendorRefOrTransactionId);
    }

    @GetMapping("/transaction/status/{queryReference}")
    public TransactionStatus getTransactionStatus(@PathVariable String queryReference) {
        log.info("> GET transaction status for: {}", queryReference);
        return mpesaPaymentService.queryTransactionStatus(queryReference);
    }

    @GetMapping("/debug/{vendorRef}")
    public ResponseEntity<MpesaPayment> getDebugInfo(@PathVariable String vendorRef) {
        log.info("> GET debug info for: {}", vendorRef);
        MpesaPayment mpesaPayment = mpesaPaymentService.getMpesaPayment(vendorRef);
        if (mpesaPayment == null) {
            mpesaPayment = mpesaPaymentService.getMpesaPaymentByTransactionId(vendorRef);
            log.debug("  no payment for vendorRef: {}, payment by transactionId={}: {}", vendorRef, vendorRef, mpesaPayment);
        }
        return new ResponseEntity<>(mpesaPayment, mpesaPayment != null ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @GetMapping("/debug/props")
    public MpesaProperties getDebugPropsInfo() {
        log.info("> GET debug props info");
        return mpesaProperties;
    }
}