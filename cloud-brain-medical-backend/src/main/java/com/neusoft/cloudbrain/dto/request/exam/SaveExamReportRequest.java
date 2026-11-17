package com.neusoft.cloudbrain.dto.request.exam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveExamReportRequest {

    @NotNull(message = "检查申请ID不能为空")
    private Long orderId;

    private String reportText;

    private String conclusion;

    private String imageUrls;
}
