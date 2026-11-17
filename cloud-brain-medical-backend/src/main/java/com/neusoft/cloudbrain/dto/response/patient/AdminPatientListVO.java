package com.neusoft.cloudbrain.dto.response.patient;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminPatientListVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private Integer gender;
    private String genderLabel;
    private String allergyHistory;
    private Integer status;
    private Long registrationCount;
    private Long medicalRecordCount;
    private LocalDateTime createdAt;
}
