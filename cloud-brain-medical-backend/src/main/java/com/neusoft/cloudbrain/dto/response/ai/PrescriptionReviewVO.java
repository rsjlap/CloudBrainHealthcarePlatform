package com.neusoft.cloudbrain.dto.response.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.neusoft.cloudbrain.common.enums.RiskLevel;
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
public class PrescriptionReviewVO {

    private RiskLevel riskLevel;
    private String overallAdvice;
    private String inventoryAdvice;
    private List<String> warnings;
    private List<DrugInteractionVO> interactions;
    private boolean aiAvailable;
    private boolean kgAvailable;
    private String kgReference;
}
