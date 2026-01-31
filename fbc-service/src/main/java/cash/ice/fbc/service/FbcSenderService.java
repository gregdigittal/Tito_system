package cash.ice.fbc.service;

import cash.ice.fbc.config.FbcProperties;
import cash.ice.fbc.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class FbcSenderService {
    private final FbcProperties fbcProperties;
    private final RestTemplate restTemplate;

    private String loginToken;

    private void obtainLoginTokenIfNeed() {
        if (loginToken == null) {
            loginToken = obtainLoginToken();
        }
    }

    private String obtainLoginToken() {
        return sendLogin(new FbcLoginRequest()
                .setUsername(fbcProperties.getUsername())
                .setPassword(fbcProperties.getPassword())).getToken();
    }

    private FbcLoginResponse sendLogin(FbcLoginRequest request) {
        log.debug("sending: {}, rl: POST {}", request, fbcProperties.getHost() + fbcProperties.getLoginUrl());
        FbcLoginResponse response = restTemplate.postForObject(
                fbcProperties.getHost() + fbcProperties.getLoginUrl(),
                request,
                FbcLoginResponse.class);
        log.info("  login response: " + response);
        return response;
    }

    public FbcVerificationResponse sendAccountVerification(String accountNumber) {
        obtainLoginTokenIfNeed();
        try {
            return sendAccountVerification(accountNumber, loginToken);
        } catch (HttpClientErrorException.Unauthorized e) {
            loginToken = obtainLoginToken();
            return sendAccountVerification(accountNumber, loginToken);
        }
    }

    private FbcVerificationResponse sendAccountVerification(String accountNumber, String token) {
        log.debug("sending acc verification, rl: GET {}, token: {}", String.format("%s%s/%s", fbcProperties.getHost(), fbcProperties.getAccountVerificationUrl(), accountNumber), Strings.isNotBlank(token));
        ResponseEntity<FbcVerificationResponse> response = restTemplate.exchange(
                String.format("%s%s/%s", fbcProperties.getHost(), fbcProperties.getAccountVerificationUrl(), accountNumber),
                HttpMethod.GET,
                new HttpEntity<>(accountNumber, createAuthHeaders(token)),
                FbcVerificationResponse.class);
        log.info("  account verification response: " + response.getBody());
        return response.getBody();
    }

    public FbcGenerateOtpResponse sendGenerateOtp(FbcGenerateOtpRequest request) {
        obtainLoginTokenIfNeed();
        try {
            return sendGenerateOtp(request, loginToken);
        } catch (HttpClientErrorException.Unauthorized e) {
            loginToken = obtainLoginToken();
            return sendGenerateOtp(request, loginToken);
        }
    }

    private FbcGenerateOtpResponse sendGenerateOtp(FbcGenerateOtpRequest request, String token) {
        log.debug("sending: {}, rl: POST {}, token: {}", request, fbcProperties.getHost() + fbcProperties.getGenerateOtpUrl(), Strings.isNotBlank(token));
        FbcGenerateOtpResponse response = restTemplate.postForObject(
                fbcProperties.getHost() + fbcProperties.getGenerateOtpUrl(),
                new HttpEntity<>(request, createAuthHeaders(token)),
                FbcGenerateOtpResponse.class);
        log.info("  generate OTP response: " + response);
        return response;
    }

    public FbcVerifyOtpResponse sendVerifyOtp(FbcVerifyOtpRequest request) {
        obtainLoginTokenIfNeed();
        try {
            return sendVerifyOtp(request, loginToken);
        } catch (HttpClientErrorException.Unauthorized e) {
            loginToken = obtainLoginToken();
            return sendVerifyOtp(request, loginToken);
        }
    }

    private FbcVerifyOtpResponse sendVerifyOtp(FbcVerifyOtpRequest request, String token) {
        log.debug("sending: {}, rl: POST {}, token: {}", request, fbcProperties.getHost() + fbcProperties.getVerifyOtpUrl(), Strings.isNotBlank(token));
        FbcVerifyOtpResponse response = restTemplate.postForObject(
                fbcProperties.getHost() + fbcProperties.getVerifyOtpUrl(),
                new HttpEntity<>(request, createAuthHeaders(token)),
                FbcVerifyOtpResponse.class);
        log.info("  verify OTP response: " + response);
        return response;
    }

    public FbcTransferSubmissionResponse sendTransferSubmission(FbcTransferSubmissionRequest request) {
        obtainLoginTokenIfNeed();
        try {
            return sendTransferSubmission(request, loginToken);
        } catch (HttpClientErrorException.Unauthorized e) {
            loginToken = obtainLoginToken();
            return sendTransferSubmission(request, loginToken);
        }
    }

    private FbcTransferSubmissionResponse sendTransferSubmission(FbcTransferSubmissionRequest request, String token) {
        log.debug("sending: {}, rl: POST {}, token: {}", request, fbcProperties.getHost() + fbcProperties.getTransferSubmissionUrl(), Strings.isNotBlank(token));
        FbcTransferSubmissionResponse response = restTemplate.postForObject(
                fbcProperties.getHost() + fbcProperties.getTransferSubmissionUrl(),
                new HttpEntity<>(request, createAuthHeaders(token)),
                FbcTransferSubmissionResponse.class);
        log.info("  Transfer Submission response: " + response);
        return response;
    }

    public FbcStatusResponse sendQueryStatus(String externalReference) {
        obtainLoginTokenIfNeed();
        try {
            return sendQueryStatus(externalReference, loginToken);
        } catch (HttpClientErrorException.Unauthorized e) {
            loginToken = obtainLoginToken();
            return sendQueryStatus(externalReference, loginToken);
        }
    }

    private FbcStatusResponse sendQueryStatus(String externalReference, String token) {
        log.debug("sending query status, rl: GET {}, token: {}", String.format("%s%s/%s", fbcProperties.getHost(), fbcProperties.getQueryStatusUrl(), externalReference), Strings.isNotBlank(token));
        ResponseEntity<FbcStatusResponse> response = restTemplate.exchange(
                String.format("%s%s/%s", fbcProperties.getHost(), fbcProperties.getQueryStatusUrl(), externalReference),
                HttpMethod.GET,
                new HttpEntity<>(externalReference, createAuthHeaders(token)),
                FbcStatusResponse.class);
        log.info("  query status response: " + response);
        return response.getBody();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }
}
