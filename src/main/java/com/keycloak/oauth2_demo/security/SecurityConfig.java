package com.keycloak.oauth2_demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;

    public SecurityConfig(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oidcUserService = oidcUserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler = 
                new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");

        http
            .securityContext(context -> context
                .securityContextRepository(new org.springframework.security.web.context.HttpSessionSecurityContextRepository())
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/login", "/register", "/public/**", "/error", "/css/**", "/js/**", "/access-denied").permitAll()
                .requestMatchers("/user").hasAnyRole("USER", "user")
                .requestMatchers("/manage").hasAnyRole("MANAGE", "manage", "MANAGER", "manager")
                .requestMatchers("/admin").hasAnyRole("ADMIN", "admin")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                // Chỉ định trang login tùy chỉnh của bạn
                // → Spring Security sẽ KHÔNG tự redirect sang Keycloak nữa
                // → Người dùng muốn đăng nhập SSO phải bấm nút "Đăng nhập qua Keycloak SSO"
                .loginPage("/login")
                // Giữ nguyên loginProcessingUrl mặc định (/login/oauth2/code/*)
                // để Keycloak vẫn redirect về được sau khi xác thực SSO
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(this.oidcUserService)
                )
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    // Khi chưa đăng nhập, redirect về trang login của bạn (không phải Keycloak)
                    response.sendRedirect(request.getContextPath() + "/login");
                })
                .accessDeniedPage("/access-denied")
            )
            .logout(logout -> logout
                .logoutSuccessHandler(oidcLogoutSuccessHandler)
                .permitAll()
            );

        return http.build();
    }
}



