package com.neusoft.cloudbrain.dto.response.equipment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EquipmentUsageRecordVO {

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String usageType;
    private String operatorName;
    private String patientInfo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String remark;
    private LocalDateTime createdAt;
}
