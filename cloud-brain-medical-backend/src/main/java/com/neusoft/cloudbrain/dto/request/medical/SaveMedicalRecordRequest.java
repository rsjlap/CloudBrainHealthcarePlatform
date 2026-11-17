package com.neusoft.cloudbrain.dto.request.medical;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveMedicalRecordRequest {

    @NotNull
    private Long registrationId;

    private String dialogText;
    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private String physicalExam;
    private String diagnosis;
    private String treatmentPlan;
    /** 可省略或留空，默认 DRAFT；有效值：DRAFT、CONFIRMED */
    private String status;
    private Integer isAiGenerated;
    private String aiDraft;
}
