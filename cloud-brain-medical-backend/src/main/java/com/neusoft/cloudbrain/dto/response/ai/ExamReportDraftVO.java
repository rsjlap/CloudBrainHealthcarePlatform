package com.neusoft.cloudbrain.dto.response.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamReportDraftVO {

    private String reportText;
    private String conclusion;
    private boolean aiAvailable;
}
