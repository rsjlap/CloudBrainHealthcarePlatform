package com.neusoft.cloudbrain.dto.request.prescription;

import com.neusoft.cloudbrain.common.enums.RiskLevel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreatePrescriptionRequest {

    @NotNull
    private Long registrationId;

    @NotEmpty
    private List<PrescriptionItemRequest> items;

    private RiskLevel riskLevel;

    private String aiReviewResult;
}
