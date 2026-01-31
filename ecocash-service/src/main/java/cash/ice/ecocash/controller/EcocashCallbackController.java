package cash.ice.ecocash.controller;

import cash.ice.ecocash.dto.EcocashCallbackPaymentResponse;
import cash.ice.ecocash.service.EcocashPaymentService;
import error.EcocashException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/ecocash/api/callback"})
@RequiredArgsConstructor
@Slf4j
public class EcocashCallbackController {
    private final EcocashPaymentService ecocashPaymentService;

    @PostMapping
    public ResponseEntity<Void> ecocashCallbackResponse(@RequestBody EcocashCallbackPaymentResponse response) {
        log.info("> callback: {}", response);
        try {
            if ("MER".equals(response.getTranType())) {
                ecocashPaymentService.callbackResponse(response);
            } else {
                log.warn("  Unknown callback response type: {}", response.getTranType());
            }

        } catch (EcocashException e) {
            ecocashPaymentService.processError(e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
