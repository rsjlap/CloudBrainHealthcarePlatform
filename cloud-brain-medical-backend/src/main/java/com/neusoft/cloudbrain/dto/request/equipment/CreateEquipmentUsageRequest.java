package com.neusoft.cloudbrain.dto.request.equipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class CreateEquipmentUsageRequest {

    @NotBlank(message = "使用类型不能为空")
    private String usageType;

    private String operatorName;
    private String patientInfo;

    @NotNull(message = "开始时间不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private String remark;
}
