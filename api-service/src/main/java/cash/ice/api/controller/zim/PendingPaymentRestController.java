package cash.ice.api.controller.zim;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentCollectionView;
import cash.ice.api.dto.PaymentView;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.PendingPaymentService;
import cash.ice.common.constant.IceCashProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping({"/api/v1/payments/pending"})
@RequiredArgsConstructor
@Profile(IceCashProfile.PROD)
@Validated
@Slf4j
public class PendingPaymentRestController {
    private final PendingPaymentService pendingPaymentService;
    private final AuthUserService authUserService;

    @GetMapping
    public Page<PaymentView> getPaymentList(@RequestParam(value = "page", defaultValue = "0") int page,
                                            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.debug("> GET payments, page={}, size={}", page, size);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.getPayments(PageRequest.of(page, size), authUser);
    }

    @GetMapping("/{id}")
    public PaymentView getPayment(@PathVariable(value = "id") Integer id) {
        log.debug("> GET payment id={}", id);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.getPayment(id, authUser);
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public PaymentView createPayment(@Valid @RequestBody PaymentView paymentView) {
        log.debug("> POST " + paymentView);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.createPayment(paymentView, authUser);
    }

    @PutMapping
    public PaymentView updatePayment(@Valid @RequestBody PaymentView paymentView) {
        log.debug("> Update {}", paymentView);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.updatePayment(paymentView, authUser);
    }

    @PostMapping(value = "/{paymentId}/upload")
    public PaymentView uploadPaymentLines(@RequestPart(value = "file") MultipartFile file,
                                          @PathVariable(value = "paymentId") @Valid @Positive String paymentId,
                                          @RequestPart(value = "template") @Valid @NotNull @NotBlank String template) throws IOException {
        log.debug("Received upload payment lines, paymentId: {}, template: {}, file: {}", paymentId, template, file);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.uploadPaymentLines(Integer.valueOf(paymentId), template, file.getInputStream(), authUser);
    }

    @PostMapping("/{paymentId}/approve")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public PaymentView approvePayment(@PathVariable(value = "paymentId") Integer paymentId) {
        log.debug("> Approve payment id={}", paymentId);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.approvePayment(paymentId, authUser);
    }

    @DeleteMapping("/{paymentId}/reject")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void rejectPayment(@PathVariable Integer paymentId) {
        log.debug("> Reject payment id={}", paymentId);
        AuthUser authUser = getAuthUser();
        pendingPaymentService.rejectPayment(paymentId, authUser);
    }

    @PostMapping("/collections")
    @ResponseStatus(code = HttpStatus.CREATED)
    public PaymentCollectionView createPaymentCollection(@Valid @RequestBody PaymentCollectionView paymentCollection) {
        log.debug("> POST " + paymentCollection);
        AuthUser authUser = getAuthUser();
        return pendingPaymentService.createPaymentCollection(paymentCollection, authUser);
    }

    @DeleteMapping("/collections/{id}/reject")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void rejectPaymentCollection(@PathVariable(value = "id") Integer id) {
        log.debug("> Reject payment collection id={}", id);
        AuthUser authUser = getAuthUser();
        pendingPaymentService.rejectPaymentCollection(id, authUser);
    }

    @PostMapping("/collections/{id}/approve")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public void approvePaymentCollection(@PathVariable(value = "id") Integer id) {
        log.debug("> Approve payment collection id={}", id);
        AuthUser authUser = getAuthUser();
        pendingPaymentService.approvePaymentCollection(id, authUser);
    }

    protected AuthUser getAuthUser() {
        return authUserService.getAuthUser();
    }
}
