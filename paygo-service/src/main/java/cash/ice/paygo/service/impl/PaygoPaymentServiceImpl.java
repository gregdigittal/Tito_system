package cash.ice.paygo.service.impl;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.service.PaymentService;
import cash.ice.common.utils.Tool;
import cash.ice.paygo.config.PaygoProperties;
import cash.ice.paygo.dto.admin.Credential;
import cash.ice.paygo.entity.PaygoMerchant;
import cash.ice.paygo.entity.PaygoPayment;
import cash.ice.paygo.error.PaygoException;
import cash.ice.paygo.repository.PaygoMerchantRepository;
import cash.ice.paygo.repository.PaygoPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static cash.ice.common.error.ErrorCodes.*;

@Service
@Slf4j
@Profile(IceCashProfile.PROD)
@RequiredArgsConstructor
public class PaygoPaymentServiceImpl implements PaymentService {
    private static final String QR64_URL_PARAMS = "?shortCode={shortCode}&narration={narration}&merchantName={merchantName}&address={address}&amount={amount}";
    private static final String DESCRIPTION = "description";

    private final RestTemplate qrRestTemplate;
    private final PaygoPaymentRepository paygoPaymentRepository;
    private final PaygoMerchantRepository paygoMerchantRepository;
    protected final PaygoProperties paygoProperties;
    private final KafkaSender kafkaSender;

    @Override
    public void processPayment(FeesData feesData, Headers headers) {
        String vendorRef = feesData.getVendorRef();
        PaymentRequest paymentRequest = feesData.getPaymentRequest();
        String deviceReference = UUID.randomUUID().toString();
        String payGoId = null;
        try {
            payGoId = generatePayGoId();
            PaygoMerchant paygoMerchant = findPaygoMerchantBy(paymentRequest.getTx());
            paygoPaymentRepository.save(new PaygoPayment()
                    .setPayGoId(payGoId)
                    .setDeviceReference(deviceReference)
                    .setMerchant(paygoMerchant.getMerchant())
                    .setCredential(findCredential(paygoMerchant, paymentRequest.getCurrency(), paymentRequest.getTx()))
                    .setExpirySeconds(paygoProperties.getRequestExpirySeconds())
                    .setPendingPayment(feesData)
                    .setKafkaHeaders(Tool.toKafkaHeaderKeys(headers)));
            String paymentDescription = getPaymentDescription(paymentRequest);
            String response = sendQr64Request(payGoId, paymentDescription, paygoMerchant, paymentRequest);
            if (response == null || !response.startsWith("data:image")) {      // ie Invalid Code
                throw new ICEcashException("Error response from PayGo DS: " + response, EC5007);
            }
            kafkaSender.sendSubPaymentResult(vendorRef, new HashMap<>(
                    Map.of("payGoId", payGoId, "deviceReference", deviceReference, "qr64", response)));

        } catch (Exception e) {
            removePendingPayment(payGoId);
            throw new PaygoException(e, payGoId, deviceReference);
        }
    }

    protected String sendQr64Request(String payGoId, String paymentDescription, PaygoMerchant paygoMerchant, PaymentRequest paymentRequest) {
        return qrRestTemplate.getForObject(
                paygoProperties.getUrl() + paygoProperties.getQr64Url() + QR64_URL_PARAMS,
                String.class,
                payGoId,
                paymentDescription,
                paygoMerchant.getMerchant().getName(),
                paygoMerchant.getMerchant().getAddressLine1(),
                paymentRequest.getAmount().intValue());
    }

    private String getPaymentDescription(PaymentRequest paymentRequest) {
        if (paymentRequest.getMeta() == null || !paymentRequest.getMeta().containsKey(DESCRIPTION)) {
            throw new ICEcashException("Payment field 'description' is absent", EC5009);
        }
        return (String) paymentRequest.getMeta().get(DESCRIPTION);
    }

    private Credential findCredential(PaygoMerchant paygoMerchant, String currency, String transactionCode) {
        return paygoMerchant.getCredentials().stream().filter(credential ->
                credential.getCurrencyCode().equals(currency)).findAny().orElseThrow(
                () -> new ICEcashException(String.format("Unknown credential for currency: %s, transaction code: %s, PaygoMerchant: %s",
                        currency, transactionCode, paygoMerchant.getId()), EC5006));
    }

    private PaygoMerchant findPaygoMerchantBy(String transactionCode) {
        return paygoMerchantRepository.findByMerchantTransactionCode(transactionCode).orElseThrow(
                () -> new ICEcashException(String.format("Unknown merchant for transaction code: %s", transactionCode),
                        EC5005));
    }

    private void removePendingPayment(String payGoId) {
        if (payGoId != null) {
            paygoPaymentRepository.findByPayGoId(payGoId).ifPresent(paygoPaymentRepository::delete);
        }
    }

    private String generatePayGoId() {
        String nulls = "0".repeat(16 - paygoProperties.getIdTotalDigits());
        String fullPrefix = paygoProperties.getPrefix() + nulls;
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String id = Tool.generateDigits(16, true, fullPrefix);
            id = paygoProperties.getPrefix() + id.substring(fullPrefix.length());

            if (!paygoPaymentRepository.existsPaygoPaymentByPayGoId(id)) {
                return id;
            }
        }
        throw new ICEcashException("Cannot generate PayGoId", EC5001);
    }

    @Override
    public void processRefund(ErrorData errorData) {
        // todo add refund logic
    }
}
