package com.neusoft.cloudbrain.dto.response.equipment;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EquipmentStatsVO {

    private long totalCount;
    private long activeCount;
    private long disabledCount;
    private long maintenanceCount;
    private long faultCount;
    private long abnormalCount;
    private List<StatusCountItem> statusDistribution;

    @Data
    @Builder
    public static class StatusCountItem {
        private String status;
        private String label;
        private long count;
    }
}
