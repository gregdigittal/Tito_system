package cash.ice.api.controller.zim;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.PaymentDocumentsService;
import cash.ice.common.constant.IceCashProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cash.ice.api.controller.zim.PendingPaymentUatRestController.authUser;

@RestController
@RequestMapping(value = {"/api/v1/payments/pending", "/api/v1/unsecure/payments/pending"})
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class PaymentDocumentsUatRestController extends PaymentDocumentsRestController {

    public PaymentDocumentsUatRestController(AuthUserService authUserService, PaymentDocumentsService paymentDocumentsService) {
        super(authUserService, paymentDocumentsService);
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
