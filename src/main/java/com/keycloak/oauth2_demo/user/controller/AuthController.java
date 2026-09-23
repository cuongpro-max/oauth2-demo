package com.keycloak.oauth2_demo.user.controller;

import com.keycloak.oauth2_demo.user.dto.LoginDto;
import com.keycloak.oauth2_demo.user.dto.RegisterDto;
import com.keycloak.oauth2_demo.user.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Hidden
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping({"/", "/login"})
    public String loginPage(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            log.info("Người dùng '{}' đã đăng nhập, chuyển hướng tới /profile", authentication.getName());
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
            log.info("Bắt đầu xử lý đăng nhập cho user: {}", loginDto.getUsername());
            userService.login(loginDto, request, response);
            log.info("Đăng nhập thành công cho user: {}", loginDto.getUsername());
            return "redirect:/profile";
        } catch (Exception e) {
            log.error("Đăng nhập thất bại cho user {}: {}", loginDto.getUsername(), e.getMessage());
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
            log.info("Bắt đầu xử lý đăng ký tài khoản cho user: {}", registerDto.getUsername());
            userService.register(registerDto);
            log.info("Tạo tài khoản Keycloak thành công: {}", registerDto.getUsername());

            LoginDto loginDto = new LoginDto(registerDto.getUsername(), registerDto.getPassword());
            userService.login(loginDto, request, response);
            log.info("Tự động đăng nhập thành công cho user mới: {}", registerDto.getUsername());

            return "redirect:/profile";
        } catch (Exception e) {
            log.error("Đăng ký tài khoản thất bại cho user {}: {}", registerDto.getUsername(), e.getMessage());
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
