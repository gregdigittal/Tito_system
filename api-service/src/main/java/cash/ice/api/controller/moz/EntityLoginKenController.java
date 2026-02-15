package cash.ice.api.controller.moz;

import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.service.EntityLoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class EntityLoginKenController {
    private final EntityLoginService entityLoginService;

    public EntityLoginKenController(@Qualifier("EntityLoginService") EntityLoginService entityLoginService) {
        this.entityLoginService = entityLoginService;
    }

    @MutationMapping
    public LoginResponse loginUserFNDS(@Argument LoginEntityRequest request) {
        log.info("> User Login (FNDS) request: {}", request);
        return entityLoginService.makeLogin(request);
    }
}
