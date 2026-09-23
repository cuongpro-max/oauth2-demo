package com.keycloak.oauth2_demo.user.controller;

import com.keycloak.oauth2_demo.user.dto.UserProfileDto;
import com.keycloak.oauth2_demo.user.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Hidden
@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public String profile(
            Model model,
            @AuthenticationPrincipal OAuth2User principal,
            HttpServletRequest request) {

        String accessToken = null;
        if (request.getSession(false) != null) {
            accessToken = (String) request.getSession(false).getAttribute("ACCESS_TOKEN");
        }

        if (principal != null || accessToken != null) {
            UserProfileDto profileDto = userService.getUserProfile(principal, accessToken);
            log.info("Truy cập trang /profile bởi user: {}", profileDto.getUsername());

            model.addAttribute("name", profileDto.getName());
            model.addAttribute("username", profileDto.getUsername());
            model.addAttribute("email", profileDto.getEmail());
            model.addAttribute("roles", profileDto.getRoles());
        }
        return "profile";
    }

    @GetMapping("/user")
    public String userPage(Model model, @AuthenticationPrincipal OAuth2User principal, HttpServletRequest request) {
        String username = extractUsername(principal, request);
        log.info("User '{}' truy cập /user thành công", username);
        model.addAttribute("username", username);
        return "user";
    }

    @GetMapping("/manage")
    public String managePage(Model model, @AuthenticationPrincipal OAuth2User principal, HttpServletRequest request) {
        String username = extractUsername(principal, request);
        log.info("User '{}' truy cập /manage thành công", username);
        model.addAttribute("username", username);
        return "manage";
    }

    @GetMapping("/admin")
    public String adminPage(Model model, @AuthenticationPrincipal OAuth2User principal, HttpServletRequest request) {
        String username = extractUsername(principal, request);
        log.info("User '{}' truy cập /admin thành công", username);
        model.addAttribute("username", username);
        return "admin";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        log.warn("Truy cập bị từ chối (403 Access Denied)");
        return "access-denied";
    }

    private String extractUsername(OAuth2User principal, HttpServletRequest request) {
        if (principal != null) {
            Object usernameObj = principal.getAttributes().get("preferred_username");
            if (usernameObj != null) return usernameObj.toString();
        }
        String accessToken = null;
        if (request.getSession(false) != null) {
            accessToken = (String) request.getSession(false).getAttribute("ACCESS_TOKEN");
        }
        UserProfileDto dto = userService.getUserProfile(principal, accessToken);
        return dto.getUsername();
    }
}
