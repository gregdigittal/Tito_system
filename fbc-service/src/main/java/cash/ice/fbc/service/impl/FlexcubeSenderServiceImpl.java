package cash.ice.fbc.service.impl;

import cash.ice.common.error.ICEcashException;
import cash.ice.fbc.config.FlexcubeProperties;
import cash.ice.fbc.dto.flexcube.*;
import cash.ice.fbc.error.FlexcubeTimeoutException;
import cash.ice.fbc.service.FlexcubeSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Instant;

import static cash.ice.common.error.ErrorCodes.EC8004;
import static cash.ice.common.error.ErrorCodes.EC8005;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlexcubeSenderServiceImpl implements FlexcubeSenderService {
    private final FlexcubeProperties flexcubeProperties;
    private final RestTemplate restTemplate;

    @Override
    public FlexcubeResponse sendPayment(FlexcubePaymentRequest request) throws FlexcubeTimeoutException {
        log.debug("  sending payment to: {}", flexcubeProperties.getUrl() + flexcubeProperties.getTransactionEndpoint());
        try {
            return restTemplate.postForObject(
                    flexcubeProperties.getUrl() + flexcubeProperties.getTransactionEndpoint(),
                    request,
                    FlexcubeResponse.class);

        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new FlexcubeTimeoutException(request.getExternalReference());
            }
            throw e;
        }
    }

    @Override
    public FlexcubeResponse sendCheck(FlexcubeStatusRequest request) {
        log.debug("  sending check to: {}, reference: {}", flexcubeProperties.getUrl() + flexcubeProperties.getPollTransactionEndpoint(), request.getIcecashReference());
        try {
            return restTemplate.postForObject(
                    flexcubeProperties.getUrl() + flexcubeProperties.getPollTransactionEndpoint(),
                    request,
                    FlexcubeResponse.class);

        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new FlexcubeTimeoutException(Integer.parseInt(request.getIcecashReference()));
            }
            throw e;
        }
    }

    @Override
    @Cacheable(value = "fbcPoolBalance", key = "{#account, #branch}")
    public FlexcubeBalanceResponse getBalance(String account, String branch) {
        log.debug("  sending GetBalance to: {}", flexcubeProperties.getUrl() + flexcubeProperties.getGetBalanceEndpoint());
        FlexcubeBalanceResponse response = restTemplate.postForObject(
                flexcubeProperties.getUrl() + flexcubeProperties.getGetBalanceEndpoint(),
                new FlexcubeBalanceRequest()
                        .setAccount(account)
                        .setBranch(branch)
                        .setUser(flexcubeProperties.getUser())
                        .setPassword(flexcubeProperties.getPassword()),
                FlexcubeBalanceResponse.class);
        if (response == null) {
            throw new ICEcashException("GetBalance no response!", EC8004);
        } else if (!"00".equals(response.getResultCode())) {
            throw new ICEcashException("GetBalance error: " + response.getResultCode() + " " +
                    response.getResultDescription(), EC8005);
        }
        response.setCreatedTime(Instant.now());
        return response;
    }

    @Override
    @CachePut(value = "fbcPoolBalance", key = "{#response.account, #response.branch}")
    public FlexcubeBalanceResponse updateBalance(FlexcubeBalanceResponse response) {
        log.debug("  updating cache, balance: {}", response.getAvailableBalance());
        return response;
    }

    @Override
    @CacheEvict(value = "fbcPoolBalance", key = "{#account, #branch}")
    public void evictBalance(String account, String branch) {
    }
}
