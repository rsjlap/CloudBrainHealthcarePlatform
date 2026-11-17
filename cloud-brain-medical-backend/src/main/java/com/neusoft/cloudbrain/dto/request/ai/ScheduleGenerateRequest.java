package com.neusoft.cloudbrain.dto.request.ai;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ScheduleGenerateRequest {

    @NotNull
    private Long deptId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
