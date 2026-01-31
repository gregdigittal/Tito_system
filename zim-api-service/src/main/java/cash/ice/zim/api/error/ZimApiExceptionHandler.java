package cash.ice.zim.api.error;

import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.zim.api.dto.ErrorResponseZim;
import jakarta.validation.ValidationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

import static cash.ice.common.error.ErrorCodes.EC1101;
import static cash.ice.common.error.ErrorCodes.EC1102;

@ControllerAdvice
@Slf4j
@AllArgsConstructor
public class ZimApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {NotFoundException.class})
    public ResponseEntity<ErrorResponseZim> handleException(NotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseZim(ex.getErrorCode(), ex.getMessage(), Tool.currentDateTime()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {ICEcashException.class})
    public ResponseEntity<ErrorResponseZim> handleException(ICEcashException ex) {
        return new ResponseEntity<>(new ErrorResponseZim(ex.getErrorCode(), ex.getMessage(), Tool.currentDateTime()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ErrorResponseZim> handleException(Exception ex) {
        log.error("Unhandled exception in: [{}], message: {}", ex.getClass().getName(), ex.getMessage(), ex);
        return new ResponseEntity<>(new ErrorResponseZim(EC1101, ex.getMessage(), Tool.currentDateTime()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {ValidationException.class})
    public ResponseEntity<Object> handleValidationException(ValidationException ex) {
        return new ResponseEntity<>(new ErrorResponseZim(EC1102, String.format("Validation failed: [%s]",
                ex.getMessage()), Tool.currentDateTime()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {ApiAuthenticationException.class})
    public ResponseEntity<Object> handleAuthenticationException(ApiAuthenticationException ignored) {
        return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
        return new ResponseEntity<>(new ErrorResponseZim(EC1102, "Validation failed: " + errors, Tool.currentDateTime()), status);
    }
}