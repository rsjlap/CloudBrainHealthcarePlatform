package com.neusoft.cloudbrain.dto.response.doctor;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DoctorProfileVO {

    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String title;
    private String departmentName;
    private String specialty;
    private String introduction;
    private BigDecimal registrationFee;
}
