package com.neusoft.cloudbrain.dto.response.registration;

import com.neusoft.cloudbrain.common.enums.TimeSlot;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ScheduleVO {

    private Long id;
    private Long doctorId;
    private LocalDate workDate;
    private TimeSlot timeSlot;
    private Integer maxPatients;
    private Integer registeredCount;
    private Integer remainingSlots;
    private Integer isAiGenerated;
}
