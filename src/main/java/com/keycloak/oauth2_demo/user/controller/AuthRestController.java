package com.keycloak.oauth2_demo.user.controller;

import com.keycloak.oauth2_demo.user.dto.AuthResponseDto;
import com.keycloak.oauth2_demo.user.dto.LoginDto;
import com.keycloak.oauth2_demo.user.dto.RegisterDto;
import com.keycloak.oauth2_demo.user.dto.UserProfileDto;
import com.keycloak.oauth2_demo.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Authentication & User API", description = "3 API chính: Đăng nhập, Đăng ký và Lấy thông tin Profile")
public class AuthRestController {

    private final UserService userService;

    @PostMapping("/auth/login")
    @Operation(summary = "1. API Đăng nhập (Login)", description = "Xác thực tài khoản với Keycloak và nhận về JWT Access Token & ID Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công, trả về JWT Token"),
            @ApiResponse(responseCode = "400", description = "Sai tên đăng nhập hoặc mật khẩu"),
            @ApiResponse(responseCode = "500", description = "Lỗi kết nối Keycloak Server")
    })
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
        log.info("[REST API] Yêu cầu đăng nhập từ user: {}", loginDto.getUsername());
        AuthResponseDto response = userService.authenticateApi(loginDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/register")
    @Operation(summary = "2. API Đăng ký (Register)", description = "Tạo tài khoản người dùng mới trên Keycloak qua Admin REST API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo tài khoản thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc mật khẩu xác nhận không khớp"),
            @ApiResponse(responseCode = "409", description = "Tên đăng nhập hoặc Email đã tồn tại")
    })
    public ResponseEntity<?> register(@RequestBody RegisterDto registerDto) {
        log.info("[REST API] Yêu cầu đăng ký tài khoản: {}", registerDto.getUsername());
        userService.register(registerDto);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Đăng ký tài khoản thành công trên Keycloak!",
                "username", registerDto.getUsername()
        ));
    }

    @GetMapping("/test/telegram-error")
    @Operation(summary = "4. API Test Cảnh báo Telegram", description = "Chủ động bắn ngoại lệ RuntimeException để kiểm tra bot Telegram gửi cảnh báo ngay tức thì")
    public ResponseEntity<?> testTelegramError() {
        log.info("[REST API] Kích hoạt giả lập lỗi để test Telegram alert");
        throw new RuntimeException("Đây là ngoại lệ giả lập (Test Telegram Alert) từ OAuth2 Demo!");
    }

    @GetMapping("/user/profile")
    @Operation(
            summary = "3. API Lấy thông tin Profile (Get Profile)",
            description = "Trích xuất thông tin người dùng và danh sách Roles từ JWT Bearer Token",
            security = @SecurityRequirement(name = "BearerJWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin profile thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc Token không hợp lệ / đã hết hạn")
    })
    public ResponseEntity<?> getProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletRequest request) {

        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        } else if (request.getSession(false) != null) {
            accessToken = (String) request.getSession(false).getAttribute("ACCESS_TOKEN");
        }

        if (principal == null && (accessToken == null || accessToken.isBlank())) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đính kèm Bearer JWT Token ở Header Authorization hoặc đăng nhập qua Session."));
        }

        UserProfileDto profileDto = userService.getUserProfile(principal, accessToken);
        return ResponseEntity.ok(profileDto);
    }
}
