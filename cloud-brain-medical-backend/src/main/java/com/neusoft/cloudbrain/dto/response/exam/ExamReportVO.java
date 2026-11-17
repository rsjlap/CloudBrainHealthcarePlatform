package com.neusoft.cloudbrain.dto.response.exam;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExamReportVO {

    private Long id;
    private Long orderId;
    private String examType;
    private String reportText;
    private String conclusion;
    private String imageUrls;
    private Integer isAiGenerated;
    private String inspectorName;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
}
