package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.enums.TimeSlot;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.dto.request.schedule.BatchPublishScheduleRequest;
import com.neusoft.cloudbrain.dto.request.schedule.CreateScheduleRequest;
import com.neusoft.cloudbrain.dto.request.schedule.UpdateScheduleRequest;
import com.neusoft.cloudbrain.dto.response.schedule.AdminScheduleVO;
import com.neusoft.cloudbrain.dto.response.schedule.BatchPublishScheduleResultVO;
import com.neusoft.cloudbrain.service.AdminScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import com.neusoft.cloudbrain.common.result.PageResult;

import java.util.List;

@Tag(name = "管理员-排班管理")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/admin/schedules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminScheduleController {

    private final AdminScheduleService adminScheduleService;

    @Operation(summary = "排班列表（按科室/日期/班次筛选、分页）")
    @GetMapping
    public Result<PageResult<AdminScheduleVO>> list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam(required = false) TimeSlot timeSlot,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(adminScheduleService.search(departmentId, workDate, timeSlot, page, size));
    }

    @Operation(summary = "批量发布排班（AI确认）")
    @PostMapping("/batch")
    public Result<BatchPublishScheduleResultVO> batchPublish(
            @Valid @RequestBody BatchPublishScheduleRequest request) {
        return Result.success(adminScheduleService.batchPublish(request));
    }

    @Operation(summary = "排班全量列表（周视图用）")
    @GetMapping("/all")
    public Result<List<AdminScheduleVO>> listAll(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam(required = false) TimeSlot timeSlot) {
        return Result.success(adminScheduleService.searchAll(departmentId, workDate, timeSlot));
    }

    @Operation(summary = "新增排班")
    @PostMapping
    public Result<AdminScheduleVO> create(@Valid @RequestBody CreateScheduleRequest request) {
        return Result.success(adminScheduleService.create(request));
    }

    @Operation(summary = "修改排班")
    @PutMapping("/{id}")
    public Result<AdminScheduleVO> update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateScheduleRequest request) {
        return Result.success(adminScheduleService.update(id, request));
    }

    @Operation(summary = "删除排班")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminScheduleService.delete(id);
        return Result.success();
    }
}
