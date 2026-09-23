package com.keycloak.oauth2_demo.user.controller;

import com.keycloak.oauth2_demo.user.dto.LoginDto;
import com.keycloak.oauth2_demo.user.dto.RegisterDto;
import com.keycloak.oauth2_demo.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            return "redirect:/profile";
        }
        if (!model.containsAttribute("loginDto")) {
            model.addAttribute("loginDto", new LoginDto());
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(
            @ModelAttribute("loginDto") LoginDto loginDto,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        try {
            userService.login(loginDto, request, response);
            return "redirect:/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", loginDto.getUsername());
            model.addAttribute("loginDto", loginDto);
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            return "redirect:/profile";
        }
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterDto());
        }
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @ModelAttribute("registerDto") RegisterDto registerDto,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        try {
            // 1. Tạo user trên Keycloak qua RestTemplate
            userService.register(registerDto);

            // 2. Tự động đăng nhập
            LoginDto loginDto = new LoginDto(registerDto.getUsername(), registerDto.getPassword());
            userService.login(loginDto, request, response);

            return "redirect:/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", registerDto.getUsername());
            model.addAttribute("email", registerDto.getEmail());
            model.addAttribute("firstName", registerDto.getFirstName());
            model.addAttribute("lastName", registerDto.getLastName());
            model.addAttribute("registerDto", registerDto);
            return "register";
        }
    }
}
