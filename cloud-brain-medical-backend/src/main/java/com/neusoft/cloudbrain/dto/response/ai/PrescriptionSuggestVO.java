package com.neusoft.cloudbrain.dto.response.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.neusoft.cloudbrain.common.enums.RiskLevel;
import com.neusoft.cloudbrain.dto.request.prescription.PrescriptionItemRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrescriptionSuggestVO {

    private RiskLevel riskLevel;
    private String overallAdvice;
    private List<String> warnings;
    private List<PrescriptionItemRequest> recommendedItems;
    private List<DrugInteractionVO> interactions;
    private boolean aiAvailable;
}
