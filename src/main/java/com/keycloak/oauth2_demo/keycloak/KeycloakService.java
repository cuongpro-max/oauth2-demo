package com.keycloak.oauth2_demo.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakService {

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    public KeycloakService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Đăng nhập qua Keycloak (Direct Grant) sử dụng RestTemplate
     */
    public Map<String, Object> authenticate(String username, String password) {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", username);
        body.add("password", password);
        body.add("scope", "openid profile email");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác.");
            }
            throw new RuntimeException("Lỗi xác thực Keycloak: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Không thể kết nối tới máy chủ Keycloak: " + e.getMessage());
        }
    }

    /**
     * Tạo tài khoản mới qua Keycloak Admin REST API sử dụng RestTemplate
     */
    public void createUser(String username, String email, String firstName, String lastName, String password) {
        String adminToken = getServiceAccountToken();

        String baseKeycloakUrl = issuerUri.substring(0, issuerUri.indexOf("/realms/"));
        String realm = issuerUri.substring(issuerUri.indexOf("/realms/") + 8);
        String createUserUrl = baseKeycloakUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", username);
        userPayload.put("email", email);
        userPayload.put("firstName", firstName);
        userPayload.put("lastName", lastName);
        userPayload.put("enabled", true);
        userPayload.put("emailVerified", true);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", password);
        credential.put("temporary", false);
        userPayload.put("credentials", List.of(credential));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(userPayload, headers);

        try {
            restTemplate.exchange(createUserUrl, HttpMethod.POST, requestEntity, Void.class);
        } catch (HttpClientErrorException.Conflict e) {
            throw new RuntimeException("Tên đăng nhập hoặc Email đã tồn tại trong hệ thống.");
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Lỗi tạo user trên Keycloak (" + e.getStatusCode() + "): Hãy kiểm tra Service Account Roles.");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối Keycloak Admin: " + e.getMessage());
        }
    }

    /**
     * Lấy token quyền quản trị (Client Credentials) bằng RestTemplate
     */
    private String getServiceAccountToken() {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("access_token")) {
                return (String) responseBody.get("access_token");
            }
            throw new RuntimeException("Không nhận được access_token từ Keycloak.");
        } catch (Exception e) {
            throw new RuntimeException("Không thể lấy Service Account Token từ Keycloak: " + e.getMessage());
        }
    }
}
