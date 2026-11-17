package com.neusoft.cloudbrain.dto.response.auth;

import com.neusoft.cloudbrain.common.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private Long expiresIn;
    private Long userId;
    private String username;
    private UserRole role;
    private String realName;
}
