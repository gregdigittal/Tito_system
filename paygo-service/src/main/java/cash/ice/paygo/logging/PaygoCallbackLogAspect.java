package cash.ice.paygo.logging;

import cash.ice.common.utils.Tool;
import cash.ice.paygo.dto.PaygoCallbackRequest;
import cash.ice.paygo.dto.PaygoCallbackResponse;
import cash.ice.paygo.entity.PaygoCallbackLog;
import cash.ice.paygo.repository.PaygoCallbackLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PaygoCallbackLogAspect {
    private final PaygoCallbackLogRepository repository;

    @Around("@annotation(cash.ice.paygo.logging.LogRequestResponse)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        PaygoCallbackLog paygoCallbackLog = new PaygoCallbackLog().setTime(Tool.currentDateTime());
        if (joinPoint.getArgs().length > 0) {
            PaygoCallbackRequest request = (PaygoCallbackRequest) joinPoint.getArgs()[0];
            paygoCallbackLog.setRequest(request);
            paygoCallbackLog.setPayGoId(request.getPayee());
            paygoCallbackLog.setMessageType(request.getMessageType());
            if (request.getResponseCode() != null) {
                paygoCallbackLog.setResponseCodeReceived(request.getResponseCode() + " " + request.getResponseDescription());
            }
            repository.save(paygoCallbackLog);
        }
        try {
            PaygoCallbackResponse response = (PaygoCallbackResponse) joinPoint.proceed();
            paygoCallbackLog.setResponse(response);
            paygoCallbackLog.setPayGoId(response.getPayee());
            paygoCallbackLog.setMessageType(response.getMessageType());
            paygoCallbackLog.setResponseCodeSent(response.getResponseCode() + " " + response.getResponseDescription());
            paygoCallbackLog.setPaymentReference(response.getPaymentReference());
            paygoCallbackLog.setDeviceReference(response.getDeviceReference());
            paygoCallbackLog.setVendorRef(response.getVendorRef());
            response.setVendorRef(null);
            repository.save(paygoCallbackLog);
            return response;
        } catch (Exception e) {
            paygoCallbackLog.setErrorMessage(e.getMessage());
            paygoCallbackLog.setErrorStackTrace(ExceptionUtils.getStackTrace(e));
            repository.save(paygoCallbackLog);
            throw e;
        }
    }
}
