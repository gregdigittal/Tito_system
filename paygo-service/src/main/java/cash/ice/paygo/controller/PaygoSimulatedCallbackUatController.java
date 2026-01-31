package cash.ice.paygo.controller;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.paygo.dto.PaygoCallbackRequest;
import cash.ice.paygo.dto.PaygoCallbackResponse;
import cash.ice.paygo.service.PaygoCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/api/paygo/simulate"})
@RequiredArgsConstructor
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class PaygoSimulatedCallbackUatController {
    private final PaygoCallbackService paygoCallbackService;

    @PostMapping
    public String simulatePaygoResponse(@RequestParam(value = "type") String type,
                                        @RequestParam(value = "payGoId") String payGoId,
                                        @RequestParam(value = "currencyCode") String currencyCode) {
        log.info("> simulate paygo response: type: {}, payGoId: {}, currencyCode: {}", type, payGoId, currencyCode);
        if ("SUCCESS".equals(type)) {
            PaygoCallbackResponse authResponse = paygoCallbackService.handleRequest(createSuccessAuth(payGoId, currencyCode));
            log.info("  formed AUTH response: " + authResponse);
            PaygoCallbackResponse adviceResponse = paygoCallbackService.handleRequest(createSuccessAdvice(payGoId));
            log.info("  formed ADVICE response: " + adviceResponse);
        }
        return "done";
    }

    private PaygoCallbackRequest createSuccessAuth(String payGoId, String currencyCode) {
        return new PaygoCallbackRequest()
                .setPayee(payGoId)
                .setMessageType("AUTH")
                .setCurrencyCode(currencyCode);
    }

    private PaygoCallbackRequest createSuccessAdvice(String payGoId) {
        return new PaygoCallbackRequest()
                .setPayee(payGoId)
                .setMessageType("ADVICE")
                .setResponseCode("000")
                .setResponseDescription("Simulated SUCCESS response");
    }
}
