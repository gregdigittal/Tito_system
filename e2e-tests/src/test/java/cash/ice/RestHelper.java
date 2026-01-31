package cash.ice;

import lombok.Getter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.lang.String.format;

public class RestHelper {
    public static final String host = "http://localhost:8281";                                // local payments
//    public static final String host = "http://localhost:8081";                                // local kyc
//    public static final String host = "http://test-mz-gateway.digittal.mobi";                 // dev payments
//    public static final String host = "http://test-kyc.digittal.mobi";                        // dev kyc
//    public static final String host = "https://uat-gateway.icecash.mobi/payments";            // uat payments

    public static final String paygoHost = "http://localhost:8285";                     // local paygo
//    public static final String paygoHost = "http://test-zw-paygo.digittal.mobi";        // dev paygo

    public static final String ecocashHost = "http://localhost:8286";                     // local paygo
//    public static final String ecocashHost = "https://uat-zw-api.icecash.mobi";        // dev paygo

    public static final String zimHost = "http://localhost:8292";                                // local zim payments
//    public static final String zimHost = "https://uat-zw-api.icecash.mobi";                      // uat payments

    private static final String urlRestPrefix = host + "/api/v1";

    @Getter
    private final RestTemplate restTemplate = new RestTemplate();

    public Wrapper sendPostRequest(String urlSuffix, String token, Map<String, Object> content) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(content, headers);
        Map<String, Object> response = restTemplate.postForObject(urlRestPrefix + urlSuffix, requestEntity, Map.class);
        return new Wrapper(response);
    }

    public Wrapper sendPostRequest(String fullUrl, BiConsumer<MultiValueMap, Map<String, Object>> requestConsumer) {
        MultiValueMap headers = new HttpHeaders();
        Map<String, Object> body = new LinkedHashMap<>();
        requestConsumer.accept(headers, body);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, requestEntity, Map.class);
        return new Wrapper(response.getBody());
    }

    public Wrapper sendPostRequest(String urlSuffix, Map<String, Object> bodyMap) {
        Map<String, Object> response = restTemplate.postForObject(urlRestPrefix + urlSuffix, bodyMap, Map.class);
        return new Wrapper(response);
    }

    public String sendSimplePostRequest(String urlSuffix, Map<String, Object> bodyMap) {
        return restTemplate.postForObject(urlRestPrefix + urlSuffix, bodyMap, String.class);
    }

    public Wrapper sendPostRequestWithParams(String urlSuffix, String params) {
        Map<String, Object> response = (Map<String, Object>) restTemplate.exchange(format("%s?%s", urlRestPrefix + urlSuffix, params), HttpMethod.POST, null, Map.class);
        return new Wrapper(response);
    }

    public Wrapper sendPostMultipartRequest(String urlSuffix, Consumer<MultiValueMap<String, Object>> bodyConsumer) {
        return sendPostMultipartRequest(urlSuffix, null, bodyConsumer);
    }

    public Wrapper sendPostMultipartRequest(String urlSuffix, String token, Consumer<MultiValueMap<String, Object>> bodyConsumer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        bodyConsumer.accept(body);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(urlRestPrefix + urlSuffix, requestEntity, Map.class);
        return new Wrapper(response.getBody());
    }

    public String sendSimplePostMultipartRequest(String urlSuffix, Consumer<MultiValueMap<String, Object>> bodyConsumer) {
        return sendSimplePostMultipartRequestFullUrl(urlRestPrefix + urlSuffix, bodyConsumer);
    }

    public String sendSimplePostMultipartRequestFullUrl(String fullUrl, Consumer<MultiValueMap<String, Object>> bodyConsumer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        bodyConsumer.accept(body);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(fullUrl, requestEntity, String.class);
        return response.getBody();
    }

    public ByteArrayResource getByteArrayResource(String internalResource, String filenameToReturn) {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(internalResource)) {
            return new ByteArrayResource(Objects.requireNonNull(inputStream).readAllBytes()) {
                @Override
                public String getFilename() {
                    return filenameToReturn;
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String sendSimpleGetRequest(String urlSuffix, String params) {
        ResponseEntity<String> responseEntity = restTemplate.exchange(urlRestPrefix + urlSuffix + (params != null ? "?" + params : ""), HttpMethod.GET, null, String.class);
        return responseEntity.getBody();
    }

    public Wrapper sendGetRequest(String urlSuffix, String params, String token) {
        HttpEntity<Map<String, Object>> httpEntity = null;
        if (token != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            httpEntity = new HttpEntity<>(headers);
        }
        ResponseEntity<Map> response = restTemplate.exchange(urlRestPrefix + urlSuffix + (params != null ? "?" + params : ""), HttpMethod.GET, httpEntity, Map.class);
        return new Wrapper(response.getBody());
    }

    public Wrapper sendGetRequest(String fullUrl, String params, Consumer<MultiValueMap> headerConsumer) {
        HttpHeaders headers = new HttpHeaders();
        headerConsumer.accept(headers);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(fullUrl + (params != null ? "?" + params : ""), HttpMethod.GET, httpEntity, Map.class);
        return new Wrapper(response.getBody());
    }

    public String sendSimpleDeleteRequest(String urlSuffix, String params) {
        ResponseEntity<String> responseEntity = restTemplate.exchange(format("%s?%s", urlRestPrefix + urlSuffix, params), HttpMethod.DELETE, null, String.class);
        return responseEntity.getBody();
    }

    public void deleteMozUser(int entityId) {
        restTemplate.delete(urlRestPrefix + format("/moz/user/%s/remove", entityId));
    }

    public void deleteProduct(int productId) {
        restTemplate.delete(urlRestPrefix + format("/ken/product/%s/remove", productId));
    }

    public void delete(String prefix, String id, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        restTemplate.exchange(urlRestPrefix + format("/%s/%s", prefix, id),
                HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
    }
}
