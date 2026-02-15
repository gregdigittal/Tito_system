package cash.ice.api.controller.moz;

import cash.ice.api.dto.moz.LinkNfcTagRequest;
import cash.ice.api.dto.moz.MozAccountInfoResponse;
import cash.ice.api.dto.moz.MozAutoRegisterDeviceRequest;
import cash.ice.api.dto.moz.TagLinkResponse;
import cash.ice.api.errors.ErrorResponse;
import cash.ice.api.errors.Me60Exception;
import cash.ice.api.service.Me60MozService;
import cash.ice.api.dto.moz.OffloadPaymentRequestMoz;
import cash.ice.api.dto.moz.PaymentRequestMoz;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping({"/api/v1/moz/me60"})
@RequiredArgsConstructor
@Validated
@Slf4j
public class Me60MozRestController {
    private final Me60MozService me60MozService;

    @PostMapping("/device/register")
    @ResponseStatus(code = OK)
    public String registerNewDevice(@Valid @RequestBody MozAutoRegisterDeviceRequest request) {
        log.info("> device register: {}", request);
        return me60MozService.registerDevice(request);
    }

    @PostMapping("/tag/link")
    @ResponseStatus(code = OK)
    public String linkTagStart(@Valid @RequestBody LinkNfcTagRequest request) {
        log.info("> link tag (1st step). DeviceSerial: {}, AccountNumber: {}, dateTime: {}", request.getDeviceSerial(), request.getAccountNumber(), request.getDateTime());
        return me60MozService.linkTag(request);
    }

    @PostMapping("/tag/link/otp")
    @ResponseStatus(code = OK)
    public TagLinkResponse linkTagValidateOtp(@Valid @RequestBody LinkNfcTagRequest request) {
        log.info("> link tag OTP (2nd step). RequestId: {}, OTP: {}", request.getRequestId(), request.getOtp());
        return me60MozService.linkTagValidateOtp(request);
    }

    @PostMapping("/tag/link/register")
    @ResponseStatus(code = OK)
    public String linkTagRegisterTag(@Valid @RequestBody LinkNfcTagRequest request) {
        log.info("> link tag register (3rd step): {}", request);
        return String.valueOf(me60MozService.linkTagRegister(request));
    }

    @GetMapping("/account/info")
    @ResponseStatus(code = OK)
    public MozAccountInfoResponse accountInfo(@Valid @RequestBody LinkNfcTagRequest request) {
        log.info("> GET account info: {}", request.getDeviceSerial());
        return me60MozService.getAccountInfo(request.getDeviceSerial());
    }

    @PostMapping("/account/info")
    @ResponseStatus(code = OK)
    public MozAccountInfoResponse accountInfoByDeviceSerial(@Valid @RequestBody LinkNfcTagRequest request) {
        log.info("> POST account info: {}", request.getDeviceSerial());
        return me60MozService.getAccountInfo(request.getDeviceSerial());
    }

    @PostMapping("/payment")
    @ResponseStatus(code = OK)
    public PaymentResponse makePayment(@Valid @RequestBody PaymentRequestMoz paymentRequestMoz) {
        log.debug("> moz payment: {}", paymentRequestMoz);
        return me60MozService.makePayment(paymentRequestMoz.toPaymentRequest());
    }

    @PostMapping("/payment/bulk")
    @ResponseStatus(code = OK)
    public List<PaymentResponse> makeBulkPayment(@Valid @RequestBody List<PaymentRequestMoz> paymentRequestMozList) {
        log.debug("> moz bulk payment: {}", paymentRequestMozList);
        return me60MozService.makeBulkPayment(paymentRequestMozList.stream().map(PaymentRequestMoz::toPaymentRequest).toList());
    }

    @PostMapping("/payment/offload")
    @ResponseStatus(code = OK)
    public PaymentResponse makeOffloadPayment(@Valid @RequestBody OffloadPaymentRequestMoz offloadPaymentRequest) {
        log.debug("> moz offload payment: {}", offloadPaymentRequest);
        return me60MozService.makeOffloadPayment(offloadPaymentRequest);
    }

    @ExceptionHandler(value = {MethodArgumentNotValidException.class, ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(@NonNull Exception ex) {
        List<String> errors = ex instanceof MethodArgumentNotValidException ev ?
                ev.getBindingResult().getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList() :
                ex instanceof ConstraintViolationException cv ?
                        cv.getConstraintViolations().stream().map(ConstraintViolation::getMessageTemplate).toList() :
                        List.of();
        return new ResponseEntity<>(
                new ErrorResponse(ErrorCodes.EC1001, errors.isEmpty() ? "Invalid request" : errors.get(0)),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {Me60Exception.class})
    public ResponseEntity<ErrorResponse> handleMe60Exception(Me60Exception ex) {
        log.warn("[{}], message: {} ({}), code: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex.getDetails(), ex.getErrorCode());
        return new ResponseEntity<>(new ErrorResponse(ex.getErrorCode(), ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {ICEcashException.class})
    public ResponseEntity<ErrorResponse> handleICEcashException(ICEcashException ex) {
        log.warn("[{}], message: {}, code: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex.getErrorCode());
        return new ResponseEntity<>(new ErrorResponse(
                ex.getErrorCode(),
                ex.isInternalError() ? "Error: " + ex.getErrorCode().substring(ex.getErrorCode().length() - 4) : ex.getMessage()
        ), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Me60 error: [{}], message: {}", ex.getClass().getName(), ex.getMessage(), ex);
        return new ResponseEntity<>(new ErrorResponse(ErrorCodes.EC1004, "Error: 1004"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}