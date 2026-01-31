package cash.ice.paygo.service.impl;

import cash.ice.paygo.config.PaygoProperties;
import cash.ice.paygo.dto.admin.*;
import cash.ice.paygo.entity.PaygoMerchant;
import cash.ice.paygo.repository.PaygoMerchantRepository;
import cash.ice.paygo.service.PaygoAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaygoAdminServiceImpl implements PaygoAdminService {
    private final PaygoMerchantRepository paygoMerchantRepository;
    private final RestTemplate restTemplate;
    private final PaygoProperties paygoProperties;

    @Override
    public List<FinancialInstitution> getFinancialInstitutions() {
        ResponseEntity<FinancialInstitution[]> response = restTemplate.exchange(
                paygoProperties.getUrl() + paygoProperties.getAdmin().getFinancialInstitutionUrl(),
                HttpMethod.GET,
                new HttpEntity<>(createApiKeyHeaders()),
                FinancialInstitution[].class);
        return response.getBody() == null ? null : Arrays.stream(response.getBody()).toList();
    }

    @Override
    public List<Merchant> getMerchants() {
        return paygoMerchantRepository.findAll().stream().map(PaygoMerchant::getMerchant).toList();
    }

    @Override
    @Transactional
    public Merchant addMerchant(MerchantCreate request) {
        PaygoMerchant paygoMerchant = paygoMerchantRepository.save(new PaygoMerchant());
        request.setMspReference(paygoMerchant.getId());
        Merchant merchant = restTemplate.postForObject(
                paygoProperties.getUrl() + paygoProperties.getAdmin().getMerchantUrl(),
                new HttpEntity<>(request, createApiKeyHeaders()),
                Merchant.class);
        Objects.requireNonNull(merchant);
        merchant.setTransactionCode(request.getTransactionCode());
        paygoMerchant.setMerchant(merchant);
        try {
            paygoMerchantRepository.save(paygoMerchant);
        } catch (Exception e) {
            deleteMerchant(merchant.getId());
            throw e;
        }
        return merchant;
    }

    @Override
    @Transactional
    public Merchant updateMerchant(Merchant merchant) {
        PaygoMerchant paygoMerchant = paygoMerchantRepository.findByMerchantId(merchant.getId()).orElseThrow();
        merchant.setMspReference(paygoMerchant.getId());
        merchant.setCreated(null);
        merchant.setUpdated(null);
        ResponseEntity<Merchant> response = restTemplate.exchange(
                paygoProperties.getUrl() + paygoProperties.getAdmin().getMerchantUrl(),
                HttpMethod.PUT,
                new HttpEntity<>(merchant, createApiKeyHeaders()),
                Merchant.class);
        if (response.getBody() != null) {
            response.getBody().setTransactionCode(merchant.getTransactionCode());
            paygoMerchantRepository.save(paygoMerchant.setMerchant(response.getBody()));
        }
        return merchant;
    }

    @Override
    public void deleteMerchant(String merchantId) {
        paygoMerchantRepository.findByMerchantId(merchantId).ifPresent(paygoMerchant -> {
            paygoMerchant.getCredentials().forEach(credential ->
                    restTemplate.exchange(
                            paygoProperties.getUrl() + paygoProperties.getAdmin().getAuthorizedCredentialUrl() +
                                    "/" + credential.getId(),
                            HttpMethod.DELETE,
                            new HttpEntity<>(createApiKeyHeaders()),
                            String.class));
            paygoMerchantRepository.delete(paygoMerchant);
        });
        restTemplate.exchange(
                paygoProperties.getUrl() + paygoProperties.getAdmin().getMerchantUrl() + "/" + merchantId,
                HttpMethod.DELETE,
                new HttpEntity<>(createApiKeyHeaders()),
                String.class);
    }

    @Override
    public List<Credential> getCredentials(String merchantId) {
        PaygoMerchant paygoMerchant = paygoMerchantRepository.findByMerchantId(merchantId).orElseThrow();
        return paygoMerchant.getCredentials();
    }

    @Override
    @Transactional
    public Credential addCredential(CredentialCreate request) {
        String merchantId = request.getMerchantId();
        request.setMerchantId(null);
        Credential credential = restTemplate.postForObject(
                paygoProperties.getUrl() + paygoProperties.getAdmin().getAuthorizedCredentialUrl(),
                new HttpEntity<>(request, createApiKeyHeaders()),
                Credential.class);
        if (credential != null) {
            credential.setMerchantId(merchantId);
            try {
                MerchantCredential merchantCredential = restTemplate.postForObject(
                        paygoProperties.getUrl() + paygoProperties.getAdmin().getMerchantCredentialsUrl(),
                        new HttpEntity<>(new MerchantCredential()
                                .setMerchantId(merchantId)
                                .setCredentialId(credential.getId()),
                                createApiKeyHeaders()),
                        MerchantCredential.class);
                if (merchantCredential != null) {
                    credential.setMerchantCredentialId(merchantCredential.getId());
                }
                PaygoMerchant paygoMerchant = paygoMerchantRepository.findByMerchantId(merchantId).orElseThrow();
                paygoMerchant.getCredentials().add(credential);
                paygoMerchantRepository.save(paygoMerchant);
            } catch (Exception e) {
                deleteCredential(credential.getId());
            }
        }
        return credential;
    }

    @Override
    @Transactional
    public void updateCredential(Credential credential) {
        PaygoMerchant paygoMerchant = paygoMerchantRepository.findByMerchantId(credential.getMerchantId()).orElseThrow();
        String merchantCredentialId = credential.getMerchantCredentialId();
        credential.setMerchantCredentialId(null);
        credential.setMerchantId(null);
        credential.setCreated(null);
        credential.setUpdated(null);
        ResponseEntity<Credential> response = restTemplate.exchange(
                paygoProperties.getUrl() + paygoProperties.getAdmin().getAuthorizedCredentialUrl(),
                HttpMethod.PUT,
                new HttpEntity<>(credential, createApiKeyHeaders()),
                Credential.class);
        if (response.getBody() != null) {
            Credential updCredential = response.getBody();
            updCredential.setMerchantId(paygoMerchant.getMerchant().getId());
            updCredential.setMerchantCredentialId(merchantCredentialId);
            paygoMerchant.getCredentials().removeIf(credential1 -> credential1.getId().equals(updCredential.getId()));
            paygoMerchant.getCredentials().add(updCredential);
            paygoMerchantRepository.save(paygoMerchant);
        }
    }

    @Override
    public void deleteCredential(String credentialId) {
        restTemplate.exchange(
                paygoProperties.getUrl() + paygoProperties.getAdmin().getAuthorizedCredentialUrl() + "/" + credentialId,
                HttpMethod.DELETE,
                new HttpEntity<>(createApiKeyHeaders()),
                String.class);
        List<PaygoMerchant> paygoMerchants = paygoMerchantRepository.extractByCredentialId(credentialId);
        if (!paygoMerchants.isEmpty()) {
            paygoMerchants.get(0).getCredentials().removeIf(credential -> credential.getId().equals(credentialId));
            paygoMerchantRepository.save(paygoMerchants.get(0));
        }
    }

    @Override
    public Expiration getPaymentExpiration(String deviceReference) {
        ResponseEntity<Expiration> response = restTemplate.exchange(
                paygoProperties.getUrl() + paygoProperties.getAdmin().getExpirationUrl() + "/" + deviceReference,
                HttpMethod.GET,
                new HttpEntity<>(createApiKeyHeaders()),
                Expiration.class);
        return response.getBody();
    }

    @Override
    public PaymentStatus getPaymentStatus(String deviceReference) {
        ResponseEntity<PaymentStatus> response = restTemplate.exchange(
                paygoProperties.getUrl() + paygoProperties.getAdmin().getPaymentStatusUrl() + "/" + deviceReference,
                HttpMethod.GET,
                new HttpEntity<>(createApiKeyHeaders()),
                PaymentStatus.class);
        return response.getBody();
    }

    private HttpHeaders createApiKeyHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(paygoProperties.getAdmin().getHeaderName(), paygoProperties.getAdmin().getHeaderValue());
        return headers;
    }
}
