package com.keycloak.oauth2_demo.user.service;

import com.keycloak.oauth2_demo.keycloak.KeycloakService;
import com.keycloak.oauth2_demo.security.JwtUtils;
import com.keycloak.oauth2_demo.user.dto.LoginDto;
import com.keycloak.oauth2_demo.user.dto.RegisterDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {

    private final KeycloakService keycloakService;
    private final JwtUtils jwtUtils;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public UserService(KeycloakService keycloakService, JwtUtils jwtUtils) {
        this.keycloakService = keycloakService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Xử lý đăng nhập: Gọi Keycloak lấy token -> Dùng JwtUtils parse Roles -> Thiết lập SecurityContext
     */
    public void login(LoginDto loginDto, HttpServletRequest request, HttpServletResponse response) {
        if (loginDto.getUsername() == null || loginDto.getUsername().isBlank() ||
                loginDto.getPassword() == null || loginDto.getPassword().isBlank()) {
            throw new RuntimeException("Vui lòng nhập đầy đủ Username và Password.");
        }

        // 1. Gọi Keycloak lấy Token qua RestTemplate
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
    }

    /**
     * Xử lý đăng ký tài khoản mới qua Keycloak Admin API
     */
    public void register(RegisterDto registerDto) {
        if (!registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp.");
        }

        keycloakService.createUser(
                registerDto.getUsername().trim(),
                registerDto.getEmail().trim(),
                registerDto.getFirstName() != null ? registerDto.getFirstName().trim() : "",
                registerDto.getLastName() != null ? registerDto.getLastName().trim() : "",
                registerDto.getPassword()
        );
    }
}
