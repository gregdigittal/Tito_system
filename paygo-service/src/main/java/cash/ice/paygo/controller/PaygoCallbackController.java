package cash.ice.paygo.controller;

import cash.ice.paygo.dto.PaygoCallbackRequest;
import cash.ice.paygo.dto.PaygoCallbackResponse;
import cash.ice.paygo.logging.LogRequestResponse;
import cash.ice.paygo.service.PaygoCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/api/paygo"})
@RequiredArgsConstructor
@Slf4j
public class PaygoCallbackController {
    private final PaygoCallbackService paygoCallbackService;

    @LogRequestResponse
    @PostMapping
    public PaygoCallbackResponse paygoRequest(@RequestBody PaygoCallbackRequest request) {
        log.info("> {} {} {} {}", request.getMessageType(), request.getPayee(), request.getResponseCode(), request);
        return paygoCallbackService.handleRequest(request);
    }
}
