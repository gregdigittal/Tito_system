package cash.ice.api.graphql;

import cash.ice.api.errors.*;
import cash.ice.common.error.ApiValidationException;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {
    private static final String ERROR_CODE = "errorCode";

    @Override
    protected GraphQLError resolveToSingleError(@NotNull Throwable ex, @NotNull DataFetchingEnvironment env) {
        GraphqlErrorBuilder<?> errorBuilder = GraphqlErrorBuilder.newError()
                .errorType(ErrorType.INTERNAL_ERROR)
                .message(ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation());
        if (ex instanceof ICEcashException e) {
            if (e.getCause() != null) {
                log.error("[{}], message: {}, code: {}", e.getClass().getSimpleName(), e.getMessage(), e.getErrorCode(), e);
            } else {
                log.warn("[{}], message: {}, code: {}", e.getClass().getSimpleName(), e.getMessage(), e.getErrorCode());
            }
            errorBuilder.extensions(Map.of(ERROR_CODE, e.getErrorCode()));
            if (e instanceof DocumentNotFoundException || e instanceof PaymentNotFoundException) {
                errorBuilder.errorType(ErrorType.NOT_FOUND);
            } else if (e instanceof ForbiddenException) {
                errorBuilder.errorType(ErrorType.FORBIDDEN);
            } else if (e instanceof UnexistingUserException) {
                errorBuilder.errorType(ErrorType.UNAUTHORIZED);
            } else if (e instanceof ApiValidationException) {
                errorBuilder.errorType(ErrorType.BAD_REQUEST);
            }
        } else if (ex instanceof ApiPaymentException e) {
            log.warn("[{}], message: {}, code: {}", e.getClass().getSimpleName(), e.getMessage(), e.getErrorCode());
            errorBuilder.extensions(Map.of(ERROR_CODE, e.getErrorCode()));
        } else if (ex instanceof NotAuthorizedException || ex instanceof AccessDeniedException) {
            log.warn("[{}], message: {}", ex.getClass().getSimpleName(), ex.getMessage());
            errorBuilder.extensions(Map.of(ERROR_CODE, ErrorCodes.EC1010));
            errorBuilder.errorType(ErrorType.UNAUTHORIZED);
        } else {
            log.error("[{}], message: {}", ex.getClass().getName(), ex.getMessage(), ex);
            errorBuilder.extensions(Map.of(ERROR_CODE, ErrorCodes.EC1004));
        }
        return errorBuilder.build();
    }
}