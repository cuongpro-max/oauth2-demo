package com.keycloak.oauth2_demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final JwtUtils jwtUtils;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository, JwtUtils jwtUtils) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.jwtUtils = jwtUtils;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");

        http
            .securityContext(context -> context
                .securityContextRepository(new HttpSessionSecurityContextRepository())
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/login", "/register", "/public/**", "/error", "/css/**", "/js/**", "/access-denied").permitAll()
                .requestMatchers("/user").hasAnyRole("USER", "user")
                .requestMatchers("/manage").hasAnyRole("MANAGE", "manage", "MANAGER", "manager")
                .requestMatchers("/admin").hasAnyRole("ADMIN", "admin")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(this.oidcUserService())
                )
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendRedirect(request.getContextPath() + "/login")
                )
                .accessDeniedPage("/access-denied")
            )
            .logout(logout -> logout
                .logoutSuccessHandler(oidcLogoutSuccessHandler)
                .permitAll()
            );

        return http.build();
    }

    /**
     * Dùng JwtUtils để bóc tách roles từ Access Token khi đăng nhập qua nút SSO Portal của Keycloak
     */
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>(oidcUser.getAuthorities());

            if (userRequest.getAccessToken() != null) {
                String token = userRequest.getAccessToken().getTokenValue();
                mappedAuthorities.addAll(jwtUtils.extractAuthorities(token));
            }

            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }
}
