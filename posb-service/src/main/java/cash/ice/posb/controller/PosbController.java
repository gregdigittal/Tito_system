package cash.ice.posb.controller;

import cash.ice.posb.config.PosbProperties;
import cash.ice.posb.dto.PosbPayment;
import cash.ice.posb.service.PosbPaymentService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class PosbController {
    private final PosbPaymentService posbPaymentService;
    private final PosbProperties posbProperties;

    @Get(value = "/debug/{vendorRef}", processes = MediaType.APPLICATION_JSON)
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<PosbPayment> getDebugInfo(@PathVariable String vendorRef) {
        log.info("> GET debug info for: {}", vendorRef);
        PosbPayment posbPayment = posbPaymentService.getPosbPayment(vendorRef);
        return posbPayment != null ? HttpResponse.ok(posbPayment) : HttpResponse.notFound();
    }

    @Get("/debug/props")
    public PosbProperties getDebugPropsInfo() {
        log.info("> GET debug props info");
        return posbProperties;
    }
}
