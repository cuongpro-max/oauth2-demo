package com.keycloak.oauth2_demo.user.controller;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class HomeController {

    @GetMapping("/profile")
    @SuppressWarnings("unchecked")
    public String profile(
            Model model,
            @AuthenticationPrincipal OAuth2User principal,
            jakarta.servlet.http.HttpServletRequest request) {

        if (principal != null) {
            model.addAttribute("name", principal.getAttribute("name"));
            model.addAttribute("username", principal.getAttribute("preferred_username"));
            model.addAttribute("email", principal.getAttribute("email"));

            Set<String> allRoles = new LinkedHashSet<>();

            // 1. Tìm roles trong ID Token / UserInfo attributes (OidcUser hoặc OAuth2User)
            extractRolesFromMap(principal.getAttributes(), allRoles);

            // 2. Nếu là OidcUser thì đọc thêm từ ID Token claims
            if (principal instanceof OidcUser oidcUser) {
                extractRolesFromMap(oidcUser.getIdToken().getClaims(), allRoles);
            }

            // 3. Tìm trong Access Token lưu ở session (do Direct Grant / form login lưu vào)
            if (request.getSession(false) != null) {
                String tokenValue = (String) request.getSession(false).getAttribute("ACCESS_TOKEN");
                if (tokenValue != null) {
                    try {
                        SignedJWT signedJWT = SignedJWT.parse(tokenValue);
                        Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
                        extractRolesFromMap(claims, allRoles);
                    } catch (Exception e) {
                        System.err.println("Không thể đọc Access Token từ session: " + e.getMessage());
                    }
                }
            }

            // Lọc bỏ các role hệ thống mặc định của Keycloak
            allRoles.removeIf(role ->
                role.startsWith("default-roles-") ||
                role.equals("offline_access") ||
                role.equals("uma_authorization") ||
                role.equals("manage-account") ||
                role.equals("manage-account-links") ||
                role.equals("view-profile")
            );

            model.addAttribute("roles", new ArrayList<>(allRoles));
        }
        return "profile";
    }

    @GetMapping("/user")
    public String userPage(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal != null) {
            model.addAttribute("username", principal.getAttribute("preferred_username"));
        }
        return "user";
    }

    @GetMapping("/manage")
    public String managePage(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal != null) {
            model.addAttribute("username", principal.getAttribute("preferred_username"));
        }
        return "manage";
    }

    @GetMapping("/admin")
    public String adminPage(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal != null) {
            model.addAttribute("username", principal.getAttribute("preferred_username"));
        }
        return "admin";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    @SuppressWarnings("unchecked")
    private void extractRolesFromMap(Map<String, Object> claims, Set<String> allRoles) {
        if (claims == null) return;

        // Realm Roles
        Object realmAccessObj = claims.get("realm_access");
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List<?> list) {
                for (Object r : list) {
                    if (r != null) allRoles.add(r.toString());
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
                            if (r != null) allRoles.add(r.toString());
                        }
                    }
                }
            }
        }

        // Direct roles attribute (nếu có custom mapper)
        Object directRoles = claims.get("roles");
        if (directRoles instanceof List<?> list) {
            for (Object r : list) {
                if (r != null) allRoles.add(r.toString());
            }
        }
    }
}