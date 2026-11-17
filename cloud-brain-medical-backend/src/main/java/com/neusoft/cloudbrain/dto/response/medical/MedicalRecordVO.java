package com.neusoft.cloudbrain.dto.response.medical;

import com.neusoft.cloudbrain.common.enums.MedicalRecordStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MedicalRecordVO {

    private Long id;
    private Long registrationId;
    private String patientName;
    private String doctorName;
    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private String dialogText;
    private String physicalExam;
    private String diagnosis;
    private String treatmentPlan;
    private Integer isAiGenerated;
    private MedicalRecordStatus status;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
}
