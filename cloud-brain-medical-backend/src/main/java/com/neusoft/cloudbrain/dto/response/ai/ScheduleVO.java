package com.neusoft.cloudbrain.dto.response.ai;

import com.neusoft.cloudbrain.common.enums.TimeSlot;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ScheduleVO {

    private Long doctorId;
    private String doctorName;
    private LocalDate workDate;
    private TimeSlot timeSlot;
    private Integer maxPatients;
    private String reason;
}
