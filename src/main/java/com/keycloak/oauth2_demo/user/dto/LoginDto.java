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
@Schema(description = "Thông tin gửi lên khi đăng nhập")
public class LoginDto {

    @Schema(description = "Tên đăng nhập", example = "user_demo")
    private String username;

    @Schema(description = "Mật khẩu", example = "123456")
    private String password;
}
