package com.keycloak.oauth2_demo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin gửi lên khi đăng ký tài khoản mới")
public class RegisterDto {

    @Schema(description = "Tên đăng nhập", example = "nguyen_van_a", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "Địa chỉ email", example = "nguyenvana@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Họ", example = "Nguyễn")
    private String firstName;

    @Schema(description = "Tên", example = "Văn A")
    private String lastName;

    @Schema(description = "Mật khẩu", example = "Password@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Xác nhận mật khẩu", example = "Password@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;
}
