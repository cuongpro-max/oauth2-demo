package com.keycloak.oauth2_demo.user.controller;

import com.keycloak.oauth2_demo.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

@Controller
public class UserController {

    private final JwtUtils jwtUtils;

    public UserController(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @GetMapping("/profile")
    public String profile(
            Model model,
            @AuthenticationPrincipal OAuth2User principal,
            HttpServletRequest request) {

        if (principal != null) {
            model.addAttribute("name", principal.getAttribute("name"));
            model.addAttribute("username", principal.getAttribute("preferred_username"));
            model.addAttribute("email", principal.getAttribute("email"));

            Set<String> cleanRoles = new LinkedHashSet<>();

            // 1. Rút roles từ attributes
            cleanRoles.addAll(jwtUtils.extractCleanRoleNames(principal.getAttributes()));

            // 2. Rút roles từ ID Token (nếu là OidcUser)
            if (principal instanceof OidcUser oidcUser) {
                cleanRoles.addAll(jwtUtils.extractCleanRoleNames(oidcUser.getIdToken().getClaims()));
            }

            // 3. Rút roles từ Access Token lưu ở session
            if (request.getSession(false) != null) {
                String accessToken = (String) request.getSession(false).getAttribute("ACCESS_TOKEN");
                if (accessToken != null) {
                    Map<String, Object> accessClaims = jwtUtils.extractClaims(accessToken);
                    cleanRoles.addAll(jwtUtils.extractCleanRoleNames(accessClaims));
                }
            }

            model.addAttribute("roles", new ArrayList<>(cleanRoles));
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
}
