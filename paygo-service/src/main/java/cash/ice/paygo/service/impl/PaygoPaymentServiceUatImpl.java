package cash.ice.paygo.service.impl;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.service.KafkaSender;
import cash.ice.paygo.config.PaygoProperties;
import cash.ice.paygo.entity.PaygoMerchant;
import cash.ice.paygo.repository.PaygoMerchantRepository;
import cash.ice.paygo.repository.PaygoPaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class PaygoPaymentServiceUatImpl extends PaygoPaymentServiceImpl {
    public PaygoPaymentServiceUatImpl(RestTemplate qrRestTemplate, PaygoPaymentRepository paygoPaymentRepository, PaygoMerchantRepository paygoMerchantRepository, PaygoProperties paygoProperties, KafkaSender kafkaSender) {
        super(qrRestTemplate, paygoPaymentRepository, paygoMerchantRepository, paygoProperties, kafkaSender);
    }

    @Override
    protected String sendQr64Request(String payGoId, String paymentDescription, PaygoMerchant paygoMerchant, PaymentRequest paymentRequest) {
        Map<String, String> simulateResponseMap = paygoProperties.getSimulateResponse();
        if (simulateResponseMap.containsKey(paymentRequest.getInitiator())) {
            return simulateQrResponse(simulateResponseMap.get(paymentRequest.getInitiator()));
        } else {
            return super.sendQr64Request(payGoId, paymentDescription, paygoMerchant, paymentRequest);
        }
    }

    private String simulateQrResponse(String simulationType) {
        if ("SUCCESS".equals(simulationType)) {
            log.debug("  simulating success response");
            return "data:image:simulatedImage";
        } else {
            return "some error";
        }
    }
}
