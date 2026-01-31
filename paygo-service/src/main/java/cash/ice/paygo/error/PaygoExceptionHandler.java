package cash.ice.paygo.error;

import cash.ice.common.error.ErrorCodes;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@ControllerAdvice
@Slf4j
@AllArgsConstructor
public class PaygoExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {HttpClientErrorException.class})
    public ResponseEntity<ErrorResponse> handleException(HttpClientErrorException ex) {
        log.error("Application error in: [{}], message: {}", ex.getClass().getName(), ex.getMessage(), ex);
        return new ResponseEntity<>(new ErrorResponse(
                Instant.now(),
                ErrorCodes.EC5003,
                ex.getResponseBodyAsString(StandardCharsets.UTF_8)
        ), ex.getStatusCode());
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unhandled exception in: [{}], message: {}", ex.getClass().getName(), ex.getMessage(), ex);
        return new ResponseEntity<>(new ErrorResponse(
                Instant.now(),
                ErrorCodes.EC5004,
                ex.getMessage()
        ), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        log.error("Not valid application error in: [{}], message: {}", ex.getClass().getName(), ex.getMessage(), ex);
        return new ResponseEntity<>(new ErrorResponse(
                Instant.now(),
                ErrorCodes.EC5002,
                ex.getMessage()
        ), status);
    }
}