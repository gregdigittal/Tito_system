package cash.ice.ecocash.service.impl;

import cash.ice.ecocash.config.EcocashProperties;
import cash.ice.ecocash.dto.EcocashCallbackPayment;
import cash.ice.ecocash.dto.EcocashCallbackPaymentResponse;
import cash.ice.ecocash.service.EcocashSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcocashSenderServiceImpl implements EcocashSenderService {
    private final EcocashProperties ecocashProperties;
    private final RestTemplate restTemplate;

    @Override
    public EcocashCallbackPaymentResponse sendPayment(EcocashCallbackPayment request) {
        return restTemplate.postForObject(
                ecocashProperties.getPaymentUrl(),
                new HttpEntity<>(request, createAuthHeaders()),
                EcocashCallbackPaymentResponse.class);
    }

    @Override
    public EcocashCallbackPaymentResponse requestPaymentStatus(String msisdn, String clientCorrelator) {
        ResponseEntity<EcocashCallbackPaymentResponse> response = restTemplate.exchange(
                ecocashProperties.getPaymentStatusUrl(msisdn, clientCorrelator),
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                EcocashCallbackPaymentResponse.class);
        return response.getBody();
    }

    @Override
    public EcocashCallbackPaymentResponse refundPayment(EcocashCallbackPayment refundRequest) {
        return restTemplate.postForObject(
                ecocashProperties.getRefundUrl(),
                new HttpEntity<>(refundRequest, createAuthHeaders()),
                EcocashCallbackPaymentResponse.class);
    }

    private HttpHeaders createAuthHeaders() {
        String auth = ecocashProperties.getHttpAuthUser() + ":" + ecocashProperties.getHttpAuthPass();
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
        String authHeader = "Basic " + new String(encodedAuth);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        return headers;
    }
}
