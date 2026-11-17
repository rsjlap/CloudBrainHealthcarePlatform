package com.neusoft.cloudbrain.dto.request.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateAdminDoctorRequest {

    private String realName;
    private String phone;
    private String password;
    private Long departmentId;
    private String title;
    private String specialty;
    private String introduction;
    private BigDecimal registrationFee;
    private Integer isAvailable;
    private Integer status;
}
