package cash.ice.api.errors;

import cash.ice.common.error.ApiValidationException;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@ControllerAdvice
@Slf4j
@AllArgsConstructor
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {ApiPaymentException.class})
    public ResponseEntity<ErrorResponse> handleException(ApiPaymentException ex) {
        log.error("Application error in: [{}], message: {}", ex.getClass().getName(), ex.getMessage(), ex);
        return new ResponseEntity<>(new ErrorResponse(
                ex.getVendorRef(),
                ex.getErrorCode(),
                ex.getMessage()
        ), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {DocumentNotFoundException.class, PaymentNotFoundException.class, JournalNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(ICEcashException ex) {
        return new ResponseEntity<>(new ErrorResponse(ex.getErrorCode(), ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {ForbiddenException.class})
    public ResponseEntity<ErrorResponse> handleException(ForbiddenException ex) {
        log.warn("Authentication exception: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse(ex.getErrorCode(), ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(value = {BulkPaymentParseException.class})
    public ResponseEntity<ErrorResponse> handleException(BulkPaymentParseException ex) {
        log.error("Application error in: [{}], message: {}", ex.getClass().getName(), ex.getMessage(), ex);
        String in = ex.getCol() != null && ex.getRow() != null ? " in " + ((char) ('A' + ex.getCol())) + ex.getRow() : "";
        return new ResponseEntity<>(new ErrorResponse(ex.getErrorCode(), ex.getMessage() + in),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {DocumentUploadingException.class, IllegalIdentifierException.class, ICEcashException.class})
    public ResponseEntity<ErrorResponse> handleException(ICEcashException ex) {
        log.error("Application error in: [{}], message: {}, code: {}", ex.getClass().getName(), ex.getMessage(), ex.getErrorCode(), ex);
        return new ResponseEntity<>(new ErrorResponse(ex.getErrorCode(), ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {MozRegistrationException.class})
    public ResponseEntity<ErrorResponse> handleMozException(MozRegistrationException ex) {
        if (ex.getCause() != null) {
            log.error("[{}], message: {}, code: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex.getErrorCode(), ex);
        }
        return new ResponseEntity<>(new ErrorResponse(ex.getErrorCode(), ex.isSendMessageToClient() ? ex.getMessage() : "Internal server error"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {KenRegistrationException.class})
    public ResponseEntity<ErrorResponse> handleKenException(KenRegistrationException ex) {
        if (ex.getCause() != null) {
            log.error("[{}], message: {}, code: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex.getErrorCode(), ex);
        }
        return new ResponseEntity<>(new ErrorResponse(ex.getErrorCode(), ex.isSendMessageToClient() ? ex.getMessage() : "Internal server error"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {UnexistingUserException.class, NotAuthorizedException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleException(RuntimeException ex) {
        log.warn("Authentication exception: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorResponse(ErrorCodes.EC1010, ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(value = {KenInternalException.class})
    public ResponseEntity<ErrorResponse> handleException(KenInternalException ex) {
        if (ex.getCause() != null) {
            log.error("[{}], message: {}, code: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex.getErrorCode(), ex);
        }
        return new ResponseEntity<>(new ErrorResponse(ex.getErrorCode(), "Internal server error"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return new ResponseEntity<>(
                new ErrorResponse(ErrorCodes.EC1004, "An internal error occurred. Please try again later."),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {ApiValidationException.class})
    public ResponseEntity<ErrorResponse> handleException(ApiValidationException ex) {
        return new ResponseEntity<>(new ErrorResponse(ex.getErrorCode(), ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleException(ConstraintViolationException ex) {
        return new ResponseEntity<>(new ErrorResponse(ErrorCodes.EC1001, "Validation failed. Please check your input."), HttpStatus.BAD_REQUEST);
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
        return new ResponseEntity<>(new ErrorResponse(ErrorCodes.EC1001, "Validation failed: " + errors), status);
    }
}