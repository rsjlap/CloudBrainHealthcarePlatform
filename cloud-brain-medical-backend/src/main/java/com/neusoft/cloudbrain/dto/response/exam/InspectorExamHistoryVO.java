package com.neusoft.cloudbrain.dto.response.exam;

import com.neusoft.cloudbrain.common.enums.ExamOrderType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InspectorExamHistoryVO {

    private Long orderId;
    private Long reportId;
    private Long registrationId;

    private String patientName;
    private String doctorName;

    private ExamOrderType orderType;
    private String examType;
    private String bodyPart;
    private String clinicalIndication;

    private String reportText;
    private String conclusion;
    private String inspectorName;
    private Integer isAiGenerated;

    private LocalDateTime orderCreatedAt;
    private LocalDateTime confirmedAt;
}
