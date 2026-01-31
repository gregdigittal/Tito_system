package cash.ice.emola.controller;

import cash.ice.common.dto.BeneficiaryNameResponse;
import cash.ice.emola.service.EmolaPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/emola"})
@RequiredArgsConstructor
@Slf4j
public class EmolaController {
    private final EmolaPaymentService emolaPaymentService;

    @GetMapping("/name/{msisdn}")
    public BeneficiaryNameResponse getEmolaNameByMsisdn(@PathVariable String msisdn) {
        log.info("> GET emola name for msisdn: " + msisdn);
        return emolaPaymentService.queryCustomerName(msisdn);
    }
}