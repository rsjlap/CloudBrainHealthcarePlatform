package com.neusoft.cloudbrain.dto.response.patient;

import com.neusoft.cloudbrain.dto.response.medical.MedicalRecordVO;
import com.neusoft.cloudbrain.dto.response.registration.RegistrationVO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminPatientDetailVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private Integer status;
    private Integer gender;
    private String genderLabel;
    private LocalDate birthDate;
    private String idCard;
    private String bloodType;
    private String allergyHistory;
    private String medicalHistory;
    private String emergencyContact;
    private String emergencyPhone;
    private LocalDateTime createdAt;
    private List<RegistrationVO> registrations;
    private List<MedicalRecordVO> medicalRecords;
}
