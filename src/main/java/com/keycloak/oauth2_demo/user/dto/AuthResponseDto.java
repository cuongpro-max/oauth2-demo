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
@Schema(description = "Kết quả trả về khi xác thực thành công")
public class AuthResponseDto {

    @Schema(description = "Thông báo kết quả", example = "Đăng nhập thành công")
    private String message;

    @Schema(description = "JWT Access Token dùng để gọi các API bảo vệ")
    private String accessToken;

    @Schema(description = "JWT ID Token chứa thông tin người dùng")
    private String idToken;

    @Schema(description = "Loại Token", example = "Bearer")
    private String tokenType;

    @Schema(description = "Thời gian hiệu lực của Token (giây)", example = "300")
    private Long expiresIn;
}
