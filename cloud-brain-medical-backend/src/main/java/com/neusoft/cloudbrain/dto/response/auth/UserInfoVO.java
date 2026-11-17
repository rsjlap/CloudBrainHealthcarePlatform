package com.neusoft.cloudbrain.dto.response.auth;

import com.neusoft.cloudbrain.common.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoVO {

    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private UserRole role;
    private String avatarUrl;
}
