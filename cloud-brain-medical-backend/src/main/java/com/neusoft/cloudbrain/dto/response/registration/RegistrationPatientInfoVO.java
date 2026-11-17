package com.neusoft.cloudbrain.dto.response.registration;

import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class RegistrationPatientInfoVO {

    private Long registrationId;
    private Long patientId;
    private String patientName;
    private String regNo;
    private LocalDate workDate;
    private Integer queueNo;
    private RegistrationStatus status;
    private Integer gender;
    private String allergyHistory;
    private String medicalHistory;
}
