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
public class EntityLoginMozController {
    private final EntityLoginService entityLoginServiceMoz;

    public EntityLoginMozController(@Qualifier("EntityLoginServiceMoz") EntityLoginService entityLoginServiceMoz) {
        this.entityLoginServiceMoz = entityLoginServiceMoz;
    }

    @MutationMapping
    public LoginResponse loginUserMoz(@Argument LoginEntityRequest request) {
        log.info("> User Login (Moz) request: {}", request);
        return entityLoginServiceMoz.makeLogin(request);
    }

    @MutationMapping
    public LoginResponse loginPosDeviceMoz(@Argument String device, @Argument String username, @Argument String password) {
        log.info("> User Login Pos Device (Moz) device: {}, username: {}", device, username);
        return entityLoginServiceMoz.makePosDeviceLogin(device, new LoginEntityRequest().setUsername(username).setPassword(password));
    }
}
