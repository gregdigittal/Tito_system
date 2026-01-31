package cash.ice.paygo.controller;

import cash.ice.paygo.dto.admin.*;
import cash.ice.paygo.service.PaygoAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = {"/api/v1/paygo", "/api/v1/unsecure/paygo"})
@RequiredArgsConstructor
@Slf4j
public class PaygoAdminController {
    private final PaygoAdminService paygoAdminService;

    @GetMapping("/financial-institutions")
    public List<FinancialInstitution> getFinancialInstitutions() {
        log.info("> PayGo GET Financial Institutions");
        return paygoAdminService.getFinancialInstitutions();
    }

    @GetMapping("/merchant")
    public List<Merchant> getMerchants() {
        log.info("> PayGo GET Merchants");
        return paygoAdminService.getMerchants();
    }

    @PostMapping("/merchant")
    public Merchant addMerchant(@Valid @RequestBody MerchantCreate request) {
        log.info("> PayGo POST Merchant request: " + request);
        return paygoAdminService.addMerchant(request);
    }

    @PutMapping("/merchant")
    public Merchant updateMerchant(@Valid @RequestBody Merchant request) {
        log.info("> PayGo PUT Merchant request: " + request);
        return paygoAdminService.updateMerchant(request);
    }

    @DeleteMapping("/merchant/{id}")
    public void deleteMerchant(@PathVariable String id) {
        log.info("> PayGo DELETE Merchant request: " + id);
        paygoAdminService.deleteMerchant(id);
    }

    @GetMapping("/merchant/credential/{merchantId}")
    public List<Credential> getCredential(@PathVariable String merchantId) {
        log.info("> PayGo GET Credentials request for merchant: " + merchantId);
        return paygoAdminService.getCredentials(merchantId);
    }

    @PostMapping("/merchant/credential")
    public Credential addCredential(@Valid @RequestBody CredentialCreate request) {
        log.info("> PayGo POST Credential request: " + request);
        return paygoAdminService.addCredential(request);
    }

    @PutMapping("/merchant/credential")
    public void updateCredential(@Valid @RequestBody Credential request) {
        log.info("> PayGo PUT Credential request: " + request);
        paygoAdminService.updateCredential(request);
    }

    @DeleteMapping("/merchant/credential/{id}")
    public void deleteCredential(@PathVariable String id) {
        log.info("> PayGo DELETE Credential request: " + id);
        paygoAdminService.deleteCredential(id);
    }

    @GetMapping("/payment/expire/{deviceReference}")
    public Expiration getPaymentExpiration(@PathVariable String deviceReference) {
        log.info("> PayGo GET payment expiration, deviceReference: " + deviceReference);
        return paygoAdminService.getPaymentExpiration(deviceReference);
    }

    @GetMapping("/payment/status/{deviceReference}")
    public PaymentStatus getPaymentStatus(@PathVariable String deviceReference) {
        log.info("> PayGo GET payment status, deviceReference: " + deviceReference);
        return paygoAdminService.getPaymentStatus(deviceReference);
    }
}
