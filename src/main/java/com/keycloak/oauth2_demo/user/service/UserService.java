package com.keycloak.oauth2_demo.user.service;

import com.keycloak.oauth2_demo.keycloak.KeycloakService;
import com.keycloak.oauth2_demo.security.JwtUtils;
import com.keycloak.oauth2_demo.user.dto.AuthResponseDto;
import com.keycloak.oauth2_demo.user.dto.LoginDto;
import com.keycloak.oauth2_demo.user.dto.RegisterDto;
import com.keycloak.oauth2_demo.user.dto.UserProfileDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final KeycloakService keycloakService;
    private final JwtUtils jwtUtils;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    /**
     * Xác thực API và trả về AuthResponseDto chứa Access Token + ID Token
     */
    public AuthResponseDto authenticateApi(LoginDto loginDto) {
        if (loginDto.getUsername() == null || loginDto.getUsername().isBlank() ||
                loginDto.getPassword() == null || loginDto.getPassword().isBlank()) {
            throw new RuntimeException("Vui lòng nhập đầy đủ Username và Password.");
        }

        Map<String, Object> tokenData = keycloakService.authenticate(
                loginDto.getUsername().trim(),
                loginDto.getPassword()
        );

        String accessToken = (String) tokenData.get("access_token");
        String idToken = (String) tokenData.get("id_token");
        String tokenType = (String) tokenData.getOrDefault("token_type", "Bearer");
        Number expiresIn = (Number) tokenData.getOrDefault("expires_in", 300);

        return AuthResponseDto.builder()
                .message("Đăng nhập thành công")
                .accessToken(accessToken)
                .idToken(idToken)
                .tokenType(tokenType)
                .expiresIn(expiresIn != null ? expiresIn.longValue() : 300L)
                .build();
    }

    /**
     * Lấy thông tin User Profile DTO từ JWT Access Token hoặc Principal
     */
    /**
     * Convert bất kỳ kiểu attribute nào về String an toàn (tránh ClassCastException với char[])
     */
    private String safeStr(Object value) {
        if (value == null) return null;
        if (value instanceof char[]) return new String((char[]) value);
        return value.toString();
    }

    public UserProfileDto getUserProfile(OAuth2User principal, String accessToken) {
        String username = null;
        String name = null;
        String email = null;
        Set<String> cleanRoles = new LinkedHashSet<>();

        if (principal != null) {
            username = safeStr(principal.getAttribute("preferred_username"));
            name = safeStr(principal.getAttribute("name"));
            email = safeStr(principal.getAttribute("email"));
            cleanRoles.addAll(jwtUtils.extractCleanRoleNames(principal.getAttributes()));

            if (principal instanceof OidcUser oidcUser) {
                cleanRoles.addAll(jwtUtils.extractCleanRoleNames(oidcUser.getIdToken().getClaims()));
            }
        }

        if (accessToken != null && !accessToken.isBlank()) {
            Map<String, Object> claims = jwtUtils.extractClaims(accessToken);
            if (username == null || username.equals("null")) {
                username = safeStr(claims.get("preferred_username"));
            }
            if (name == null || name.equals("null")) {
                name = safeStr(claims.get("name"));
            }
            if (email == null || email.equals("null")) {
                email = safeStr(claims.get("email"));
            }
            cleanRoles.addAll(jwtUtils.extractCleanRoleNames(claims));
        }

        return UserProfileDto.builder()
                .username(username != null && !username.equals("null") ? username : "N/A")
                .name(name != null && !name.equals("null") ? name : "N/A")
                .email(email != null && !email.equals("null") ? email : "N/A")
                .roles(new ArrayList<>(cleanRoles))
                .build();
    }

    /**
     * Xử lý đăng nhập Form HTML: Gọi Keycloak lấy token -> Dùng JwtUtils parse Roles -> Thiết lập SecurityContext
     */
    public void login(LoginDto loginDto, HttpServletRequest request, HttpServletResponse response) {
        if (loginDto.getUsername() == null || loginDto.getUsername().isBlank() ||
                loginDto.getPassword() == null || loginDto.getPassword().isBlank()) {
            throw new RuntimeException("Vui lòng nhập đầy đủ Username và Password.");
        }

        // 1. Gọi Keycloak lấy Token qua RestTemplate
        log.debug("Gọi Keycloak authenticate cho user: {}", loginDto.getUsername());
        Map<String, Object> tokenData = keycloakService.authenticate(
                loginDto.getUsername().trim(),
                loginDto.getPassword()
        );

        String accessToken = (String) tokenData.get("access_token");
        String idTokenStr = (String) tokenData.get("id_token");

        if (accessToken == null) {
            throw new RuntimeException("Không nhận được access_token từ Keycloak.");
        }

        // 2. Dùng JwtUtils đọc Claims và Roles từ Token
        Map<String, Object> claims = jwtUtils.extractClaims(idTokenStr != null ? idTokenStr : accessToken);
        Set<GrantedAuthority> authorities = jwtUtils.extractAuthorities(accessToken);

        log.info("Xác thực JWT thành công! User: '{}', Granted Authorities: {}", loginDto.getUsername(), authorities);

        // 3. Dựng Principal OidcUser
        OidcIdToken oidcIdToken = new OidcIdToken(
                idTokenStr != null ? idTokenStr : accessToken,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
        );
        OidcUser oidcUser = new DefaultOidcUser(authorities, oidcIdToken, "preferred_username");

        // 4. Tạo Authentication và lưu vào SecurityContext
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oidcUser,
                authorities,
                "keycloak"
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Lưu vào Session để Spring Security duy trì đăng nhập
        securityContextRepository.saveContext(context, request, response);

        HttpSession session = request.getSession(true);
        session.setAttribute("ACCESS_TOKEN", accessToken);
        log.debug("Đã lưu SecurityContext & Access Token vào Session ID: {}", session.getId());
    }

    /**
     * Xử lý đăng ký tài khoản mới qua Keycloak Admin API
     */
    public void register(RegisterDto registerDto) {
        if (!registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp.");
        }

        log.info("Tạo tài khoản Keycloak cho: username='{}', email='{}'", registerDto.getUsername(), registerDto.getEmail());
        keycloakService.createUser(
                registerDto.getUsername().trim(),
                registerDto.getEmail().trim(),
                registerDto.getFirstName() != null ? registerDto.getFirstName().trim() : "",
                registerDto.getLastName() != null ? registerDto.getLastName().trim() : "",
                registerDto.getPassword()
        );
    }
}
