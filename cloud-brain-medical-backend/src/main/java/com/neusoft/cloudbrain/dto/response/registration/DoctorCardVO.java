package com.neusoft.cloudbrain.dto.response.registration;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DoctorCardVO {

    private Long id;
    private String realName;
    private String title;
    private String specialty;
    private BigDecimal registrationFee;
    private String departmentName;
    private List<String> scheduleSlots;
}
