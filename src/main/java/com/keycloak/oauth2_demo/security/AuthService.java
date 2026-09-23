package com.keycloak.oauth2_demo.security;

import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;

@Service
public class AuthService {

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    private final RestClient restClient = RestClient.create();

    private final org.springframework.security.web.context.SecurityContextRepository securityContextRepository = 
            new HttpSessionSecurityContextRepository();

    /**
     * Xác thực người dùng qua Direct Grant (Resource Owner Password Credentials) của Keycloak
     */
    @SuppressWarnings("unchecked")
    public void login(String username, String password, HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", username);
        formData.add("password", password);
        formData.add("scope", "openid profile email");

        Map<String, Object> tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác.");
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new RuntimeException("Không nhận được token từ Keycloak.");
        }

        String accessToken = (String) tokenResponse.get("access_token");
        String idTokenStr = (String) tokenResponse.get("id_token");

        try {
            SignedJWT accessJwt = SignedJWT.parse(accessToken);
            Map<String, Object> accessClaims = accessJwt.getJWTClaimsSet().getClaims();

            Map<String, Object> idClaims = accessClaims;
            if (idTokenStr != null) {
                SignedJWT idJwt = SignedJWT.parse(idTokenStr);
                idClaims = idJwt.getJWTClaimsSet().getClaims();
            }

            Set<GrantedAuthority> authorities = new HashSet<>();
            extractKeycloakAuthorities(accessClaims, authorities);
            extractKeycloakAuthorities(idClaims, authorities);

            OidcIdToken idToken = new OidcIdToken(
                    idTokenStr != null ? idTokenStr : accessToken,
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    idClaims
            );

            OidcUser oidcUser = new DefaultOidcUser(authorities, idToken, "preferred_username");

            OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                    oidcUser,
                    authorities,
                    "keycloak"
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // Lưu context vào session để Spring Security duy trì đăng nhập
            securityContextRepository.saveContext(context, request, response);

            HttpSession session = request.getSession(true);
            session.setAttribute("ACCESS_TOKEN", accessToken);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xử lý phiên đăng nhập: " + e.getMessage());
        }
    }

    /**
     * Tạo tài khoản mới qua Keycloak Admin REST API
     */
    public void register(String username, String email, String firstName, String lastName, String password) {
        // 1. Lấy Admin/Service Account Token
        String adminToken = getServiceAccountToken();

        // 2. Gọi Admin API để tạo User
        String baseKeycloakUrl = issuerUri.substring(0, issuerUri.indexOf("/realms/"));
        String realm = issuerUri.substring(issuerUri.indexOf("/realms/") + 8);
        String createUserUrl = baseKeycloakUrl + "/admin/realms/" + realm + "/users";

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

        try {
            restClient.post()
                    .uri(createUserUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userPayload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        if (resp.getStatusCode().value() == 409) {
                            throw new RuntimeException("Tên đăng nhập hoặc Email đã tồn tại trong hệ thống.");
                        }
                        throw new RuntimeException("Lỗi từ Keycloak Admin API (" + resp.getStatusCode() + "). Vui lòng kiểm tra quyền Service Account.");
                    })
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private String getServiceAccountToken() {
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new RuntimeException("Không thể lấy Service Account Token từ Keycloak. Hãy kiểm tra Client authentication & Service accounts roles.");
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && response.containsKey("access_token")) {
                return (String) response.get("access_token");
            }
            throw new RuntimeException("Không tìm thấy access_token từ Keycloak response.");
        } catch (Exception e) {
            throw new RuntimeException("Không thể xác thực quyền quản trị: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void extractKeycloakAuthorities(Map<String, Object> claims, Set<GrantedAuthority> authorities) {
        if (claims == null) return;

        // Realm Roles
        Object realmAccessObj = claims.get("realm_access");
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List<?> list) {
                for (Object r : list) {
                    if (r != null) {
                        String role = r.toString();
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                }
            }
        }

        // Client Roles
        Object resourceAccessObj = claims.get("resource_access");
        if (resourceAccessObj instanceof Map<?, ?> resourceAccess) {
            for (Object clientObj : resourceAccess.values()) {
                if (clientObj instanceof Map<?, ?> clientMap) {
                    Object rolesObj = clientMap.get("roles");
                    if (rolesObj instanceof List<?> list) {
                        for (Object r : list) {
                            if (r != null) {
                                String role = r.toString();
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                                authorities.add(new SimpleGrantedAuthority(role));
                            }
                        }
                    }
                }
            }
        }
    }
}
