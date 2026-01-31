package cash.ice.api.service.impl;

import cash.ice.api.entity.zim.Payment;
import cash.ice.api.service.PaymentExecutionService;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.PaymentLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cash.ice.common.error.ErrorCodes.EC1026;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentExecutionServiceImpl implements PaymentExecutionService {
    private static final String VENDOR_PREFIX = "auto_";
    private static final String API_VERSION = "1";
    private static final DateTimeFormatter VENDOR_FORMATTER = DateTimeFormatter.ofPattern("yyMMddhhmmss");

    private final KafkaSender kafkaSender;

    @Override
    public void execute(Payment payment, List<PaymentLine> paymentLines) {
        log.debug("  execute payment: {}, with {} lines", payment.getId(), paymentLines.size());
        paymentLines.forEach(line -> {
            PaymentRequest paymentRequest = createPaymentRequest(payment, line);
            sendPaymentRequest(paymentRequest);
        });
    }

    private PaymentRequest createPaymentRequest(Payment payment, PaymentLine line) {
        int referenceId = Integer.parseInt(Tool.generateDigits(7, false));
        PaymentRequest paymentRequest = new PaymentRequest()
                .setVendorRef(VENDOR_PREFIX + Tool.currentDateTime().format(VENDOR_FORMATTER) + referenceId)
                .setApiVersion(API_VERSION)
                .setAmount(line.getAmount())
                .setCurrency(line.getCurrency())
                .setTx(line.getTransactionCode())
                .setMeta(new HashMap<>(line.getMeta() != null ? line.getMeta() : Map.of()))
                .setPartnerId(payment.getMeta() == null ? null : (String) payment.getMeta().get("partnerId"))
                .setDeviceId(payment.getMeta() == null ? null : (String) payment.getMeta().get("deviceId"))
                .setDate(Tool.currentDateTime());
        paymentRequest.getMeta().putAll(Map.of(
                "description", line.getDetails() != null ? line.getDetails() : "",
                "paymentId", payment.getId(),
                "paymentLineId", line.getId()));

        switch ((String) line.getMeta().get("paymentMethod")) {
            case "RTGS" -> {
                paymentRequest.setInitiatorType("icecash")
                        .setInitiator(String.valueOf(payment.getAccountId()));
                paymentRequest.getMeta().put("referenceId", referenceId);
                paymentRequest.getMeta().putIfAbsent("beneficiaryAddress", "");
            }
            case "PayGo", "EcoCash", "Netone" ->
                    paymentRequest.setInitiatorType(((String) line.getMeta().get("paymentMethod")).toLowerCase())
                            .setInitiator((String) payment.getMeta().get("initiator"));
            default -> throw new ICEcashException("Unsupported payment method", EC1026);
        }
        log.debug("  paymentMethod: {}, created request: {}", line.getMeta().get("paymentMethod"), paymentRequest);
        return paymentRequest;
    }

    private void sendPaymentRequest(PaymentRequest paymentRequest) {
        log.info("  sending payment: " + paymentRequest);
        kafkaSender.sendPaymentRequest(paymentRequest.getVendorRef(), paymentRequest);
    }
}
