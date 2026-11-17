package com.neusoft.cloudbrain.dto.response.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiScheduleGenerateResultVO {

    private List<ScheduleVO> schedules;
    private boolean aiAvailable;
    private String summary;
}
