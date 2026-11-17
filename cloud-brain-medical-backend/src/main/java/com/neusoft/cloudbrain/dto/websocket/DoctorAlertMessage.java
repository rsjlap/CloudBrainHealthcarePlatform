package com.neusoft.cloudbrain.dto.websocket;

import com.neusoft.cloudbrain.common.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAlertMessage {

    private String type;

    private String title;

    private RiskLevel riskLevel;

    private String message;

    private String overallAdvice;

    private Long patientId;

    private List<String> warnings;

    private LocalDateTime timestamp;
}
