package com.neusoft.cloudbrain.dto.response.prescription;

import com.neusoft.cloudbrain.common.enums.PrescriptionStatus;
import com.neusoft.cloudbrain.common.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PrescriptionVO {

    private Long id;
    private Long registrationId;
    private String patientName;
    private String doctorName;
    private PrescriptionStatus status;
    private String aiReviewResult;
    private RiskLevel riskLevel;
    private String dispensedByName;
    private LocalDateTime dispensedAt;
    private Boolean paid;
    private LocalDateTime createdAt;
    private List<PrescriptionItemVO> items;
}
