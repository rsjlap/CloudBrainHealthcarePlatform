package com.neusoft.cloudbrain.dto.response.schedule;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchPublishScheduleResultVO {

    private int successCount;
    private int skippedCount;
    private List<String> errors;
}
