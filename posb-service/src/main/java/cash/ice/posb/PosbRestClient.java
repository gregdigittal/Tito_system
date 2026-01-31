package cash.ice.posb;

import cash.ice.posb.dto.posb.*;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

@Client("${ice.cash.posb.host}")
@Header(name = "X-API-Key", value = "${ice.cash.posb.api-key}")
public interface PosbRestClient {

    @Post("${ice.cash.posb.instruction-url}")
    @NonNull PosbInstructionResponse sendInstruction(
            @Body PosbInstructionRequest request,
            @Header("X-Trace-ID") String apiKey
    );

    @Post("${ice.cash.posb.confirmation-url}")
    PosbConfirmationResponse sendConfirmation(
            @Body PosbConfirmationRequest request,
            @Header("X-Trace-ID") String apiKey
    );

    @Get("${ice.cash.posb.status-url}")
    PosbStatusResponse getPaymentStatus(String paymentReference);

    @Post("${ice.cash.posb.reversal-url}")
    PosbReversalResponse sendReversal(
            @Body PosbReversalRequest request,
            @Header("X-Trace-ID") String apiKey
    );
}
