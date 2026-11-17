package com.neusoft.cloudbrain.dto.request.medical;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefineTreatmentPlanRequest {

    @NotNull
    private Long registrationId;

    /** 签名时的初步处理意见 */
    private String treatmentPlan;

    private String diagnosis;
}
