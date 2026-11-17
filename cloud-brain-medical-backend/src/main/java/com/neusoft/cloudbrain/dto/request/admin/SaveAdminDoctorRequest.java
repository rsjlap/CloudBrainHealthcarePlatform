package com.neusoft.cloudbrain.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaveAdminDoctorRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private String phone;

    @NotNull(message = "科室不能为空")
    private Long departmentId;

    private String title;

    private String specialty;

    private String introduction;

    private BigDecimal registrationFee;
}
