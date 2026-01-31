package cash.ice.zim.api.aop;

import cash.ice.zim.api.config.ZimApiProperties;
import cash.ice.zim.api.error.ApiAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Slf4j
@Component
@Aspect
@RequiredArgsConstructor
public class ApiKeyCheckAspect {
    private final ZimApiProperties zimApiProperties;

    private static final String[] IP_HEADERS = {
            "CF-Connecting-IP",
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    @Around("@annotation(cash.ice.zim.api.aop.ApiKeyRestricted)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        if (zimApiProperties.isApiKeyCheck()) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (servletRequestAttributes != null) {
                String apiKey = servletRequestAttributes.getRequest().getHeader("Api-Key");
                String actualApiKey = zimApiProperties.getApiKey();
                ApiKeyRestricted apiKeyAnno = ((MethodSignature) joinPoint.getSignature()).getMethod().getAnnotation(ApiKeyRestricted.class);
                if (apiKeyAnno != null && apiKeyAnno.internal()) {
                    actualApiKey = zimApiProperties.getApiKeyInternal();
                }
                if (Objects.equals(apiKey, actualApiKey)) {
                    return joinPoint.proceed();
                } else {
                    log.debug("'Api-Key' header is wrong: {}, required: {}, endpoint: {}, remoteAddr: {}", apiKey,
                            zimApiProperties.getApiKey(), joinPoint.getSignature(), getRequestIP(servletRequestAttributes.getRequest()));
                }
            }
            throw new ApiAuthenticationException();
        } else {
            return joinPoint.proceed();
        }
    }

    public static String getRequestIP(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (value == null || value.isEmpty()) {
                continue;
            }
            String[] parts = value.split("\\s*,\\s*");
            return parts[0];
        }
        return request.getRemoteAddr();
    }
}
