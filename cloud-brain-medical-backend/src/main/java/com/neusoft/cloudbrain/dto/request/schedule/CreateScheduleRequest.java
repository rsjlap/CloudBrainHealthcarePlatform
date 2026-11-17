package com.neusoft.cloudbrain.dto.request.schedule;

import com.neusoft.cloudbrain.common.enums.TimeSlot;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class CreateScheduleRequest {

    @NotNull(message = "请选择医生")
    private Long doctorId;

    @NotNull(message = "请选择日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate workDate;

    @NotNull(message = "请选择班次")
    private TimeSlot timeSlot;

    @NotNull(message = "请设置号源上限")
    @Min(value = 1, message = "号源上限至少为 1")
    private Integer maxPatients;
}
