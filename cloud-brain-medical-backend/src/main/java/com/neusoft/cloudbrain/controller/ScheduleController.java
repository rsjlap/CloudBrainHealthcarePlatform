package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.dto.response.registration.ScheduleVO;
import com.neusoft.cloudbrain.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "排班模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "医生排班时段")
    @GetMapping
    public Result<List<ScheduleVO>> list(@RequestParam Long doctorId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(scheduleService.listByDoctor(doctorId, date));
    }

    @Operation(summary = "可用号源")
    @GetMapping("/available")
    public Result<List<ScheduleVO>> available(@RequestParam Long doctorId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(scheduleService.getAvailableSlots(doctorId, date));
    }
}
