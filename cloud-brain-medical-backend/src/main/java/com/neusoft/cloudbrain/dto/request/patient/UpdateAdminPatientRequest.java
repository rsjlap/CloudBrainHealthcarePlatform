package com.neusoft.cloudbrain.dto.request.patient;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class UpdateAdminPatientRequest {

    private String realName;
    private String phone;
    private Integer gender;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    private String idCard;
    private String bloodType;
    private String allergyHistory;
    private String medicalHistory;
    private String emergencyContact;
    private String emergencyPhone;
    private Integer status;
}
