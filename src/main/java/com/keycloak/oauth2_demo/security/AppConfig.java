package com.keycloak.oauth2_demo.security;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class AppConfig {

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>(oidcUser.getAuthorities());

            // Extract the roles
            if (userRequest.getAccessToken() != null) {
                String accessToken = userRequest.getAccessToken().getTokenValue();
                Set<GrantedAuthority> keycloakRoles = extractKeycloakRoles(accessToken);
                mappedAuthorities.addAll(keycloakRoles);
            }

            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }

    private Set<GrantedAuthority> extractKeycloakRoles(String accessToken) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        try {
            // Decode the access token
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();

            // 1. Realm Roles (realm_access.roles)
            Object realmAccessObj = claims.get("realm_access");
            if (realmAccessObj instanceof Map<?, ?> realmAccess) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof List<?> list) {
                    for (Object r : list) {
                        if (r != null) {
                            String role = r.toString();
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        }
                    }
                }
            }

            // 2. Client Roles (resource_access.<client>.roles)
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
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi giải mã token: " + e.getMessage());
        }

        return authorities;
    }
}
