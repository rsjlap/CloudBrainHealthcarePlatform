package com.neusoft.cloudbrain.dto.response.admin;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminDoctorListVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private Integer status;
    private Long departmentId;
    private String departmentName;
    private String title;
    private String specialty;
    private String introduction;
    private BigDecimal registrationFee;
    private Integer isAvailable;
    private LocalDateTime createdAt;
}
