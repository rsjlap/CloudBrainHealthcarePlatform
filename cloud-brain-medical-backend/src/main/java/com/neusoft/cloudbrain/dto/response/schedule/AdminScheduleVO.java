package com.neusoft.cloudbrain.dto.response.schedule;

import com.neusoft.cloudbrain.common.enums.TimeSlot;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AdminScheduleVO {

    private Long id;
    private Long doctorId;
    private String doctorName;
    private String doctorTitle;
    private Long departmentId;
    private String departmentName;
    private Boolean doctorAvailable;
    private LocalDate workDate;
    private TimeSlot timeSlot;
    private Integer maxPatients;
    private Integer registeredCount;
    private Integer remainingSlots;
    private String slotStatus;
    private String dutyStatus;
    private Integer isAiGenerated;
}
