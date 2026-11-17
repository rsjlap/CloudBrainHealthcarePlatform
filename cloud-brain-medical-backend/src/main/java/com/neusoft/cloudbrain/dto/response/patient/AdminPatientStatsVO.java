package com.neusoft.cloudbrain.dto.response.patient;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminPatientStatsVO {

    private long totalCount;
    private long maleCount;
    private long femaleCount;
    private long allergyCount;
    private long todayNewCount;
    private long visitedCount;
    private long activeCount;
}
