package cash.ice.api.controller.zim;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.PendingPaymentService;
import cash.ice.common.constant.IceCashProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/payments/pending", "/api/v1/unsecure/payments/pending"})
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class PendingPaymentUatRestController extends PendingPaymentRestController {
    static AuthUser authUser;

    public PendingPaymentUatRestController(PendingPaymentService pendingPaymentService, AuthUserService authUserService) {
        super(pendingPaymentService, authUserService);
    }

    @GetMapping("/auth")
    public AuthUser authenticateTestUser() {
        return authUser;
    }

    @PostMapping("/auth")
    public AuthUser authenticateTestUser(@RequestBody AuthUser authUser) {
        log.debug("> Authenticating: " + authUser);
        PendingPaymentUatRestController.authUser = authUser;
        return authUser;
    }

    @DeleteMapping("/auth")
    public void clearTestUserAuth() {
        log.debug("> Clear authentication.");
        PendingPaymentUatRestController.authUser = null;
    }

    @Override
    protected AuthUser getAuthUser() {
        if (authUser != null) {
            log.info("Replacing auth token to: " + authUser);
            return authUser;
        } else {
            return super.getAuthUser();
        }
    }
}
