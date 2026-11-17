package com.neusoft.cloudbrain.dto.request.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchPublishScheduleRequest {

    @NotEmpty(message = "排班列表不能为空")
    @Valid
    private List<CreateScheduleRequest> items;
}
