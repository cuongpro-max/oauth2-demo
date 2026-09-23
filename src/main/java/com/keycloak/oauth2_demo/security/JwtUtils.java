package com.keycloak.oauth2_demo.security;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JwtUtils {

    /**
     * Giải mã JWT Token và lấy toàn bộ claims
     */
    public Map<String, Object> extractClaims(String token) {
        if (token == null || token.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getClaims();
        } catch (Exception e) {
            System.err.println("Lỗi giải mã JWT: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Trích xuất các roles từ token (cả Realm Roles và Client Roles)
     * Thêm tiền tố ROLE_ để Spring Security nhận diện (hasRole)
     */
    @SuppressWarnings("unchecked")
    public Set<GrantedAuthority> extractAuthorities(String token) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        Map<String, Object> claims = extractClaims(token);

        // 1. Realm Roles (nằm trong claims: realm_access.roles)
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

        // 2. Client Roles (nằm trong claims: resource_access.<client>.roles)
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

        return authorities;
    }

    /**
     * Lấy danh sách tên Role sạch để hiển thị trên giao diện Profile
     */
    @SuppressWarnings("unchecked")
    public List<String> extractCleanRoleNames(Map<String, Object> claims) {
        Set<String> roles = new LinkedHashSet<>();
        if (claims == null) return Collections.emptyList();

        // Lấy Realm roles
        Object realmAccess = claims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof List<?> list) {
            for (Object r : list) if (r != null) roles.add(r.toString());
        }

        // Lấy Client roles
        Object resAccess = claims.get("resource_access");
        if (resAccess instanceof Map<?, ?> map) {
            for (Object client : map.values()) {
                if (client instanceof Map<?, ?> clientMap && clientMap.get("roles") instanceof List<?> list) {
                    for (Object r : list) if (r != null) roles.add(r.toString());
                }
            }
        }

        // Bỏ bớt các role mặc định hệ thống của Keycloak
        roles.removeIf(r -> r.startsWith("default-roles-") ||
                r.equals("offline_access") ||
                r.equals("uma_authorization") ||
                r.equals("manage-account") ||
                r.equals("manage-account-links") ||
                r.equals("view-profile"));

        return new ArrayList<>(roles);
    }
}
