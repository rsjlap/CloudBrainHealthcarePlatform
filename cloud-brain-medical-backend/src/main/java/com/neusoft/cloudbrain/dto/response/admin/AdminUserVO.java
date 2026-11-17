package com.neusoft.cloudbrain.dto.response.admin;

import com.neusoft.cloudbrain.common.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private UserRole role;
    private Integer status;
    private LocalDateTime createdAt;
}
