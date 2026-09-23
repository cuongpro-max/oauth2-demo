package com.keycloak.oauth2_demo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin chi tiết Profile của người dùng")
public class UserProfileDto {

    @Schema(description = "Tên đăng nhập", example = "cuong_pro")
    private String username;

    @Schema(description = "Họ và tên đầy đủ", example = "Nguyễn Văn Cường")
    private String name;

    @Schema(description = "Địa chỉ Email", example = "cuong@example.com")
    private String email;

    @Schema(description = "Danh sách Role được gán từ Keycloak", example = "[\"user\", \"admin\"]")
    private List<String> roles;
}
