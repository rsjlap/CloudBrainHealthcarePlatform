package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.common.util.SecurityContextUtil;
import com.neusoft.cloudbrain.config.SwaggerConfig;
import com.neusoft.cloudbrain.dto.request.doctor.UpdateDoctorProfileRequest;
import com.neusoft.cloudbrain.dto.response.doctor.DoctorProfileVO;
import com.neusoft.cloudbrain.dto.response.registration.DoctorCardVO;
import com.neusoft.cloudbrain.dto.response.registration.ScheduleVO;
import com.neusoft.cloudbrain.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "医生模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @Operation(summary = "当前医生个人资料")
    @SecurityRequirement(name = SwaggerConfig.AUTH_SCHEME)
    @GetMapping("/me/profile")
    public Result<DoctorProfileVO> myProfile() {
        return Result.success(doctorService.getProfile(SecurityContextUtil.getCurrentUserId()));
    }

    @Operation(summary = "更新当前医生专长简介")
    @SecurityRequirement(name = SwaggerConfig.AUTH_SCHEME)
    @PutMapping("/me/profile")
    public Result<DoctorProfileVO> updateProfile(@Valid @RequestBody UpdateDoctorProfileRequest request) {
        return Result.success(doctorService.updateProfile(SecurityContextUtil.getCurrentUserId(), request));
    }

    @Operation(summary = "当前医生排班（日期区间）")
    @SecurityRequirement(name = SwaggerConfig.AUTH_SCHEME)
    @GetMapping("/me/schedules")
    public Result<List<ScheduleVO>> mySchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(doctorService.listMySchedules(
                SecurityContextUtil.getCurrentUserId(), startDate, endDate));
    }

    @Operation(summary = "医生列表")
    @GetMapping
    public Result<List<DoctorCardVO>> list(@RequestParam Long deptId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(doctorService.listByDepartment(deptId, date));
    }

    @Operation(summary = "医生详情")
    @GetMapping("/{id}")
    public Result<DoctorCardVO> getById(@PathVariable Long id) {
        return Result.success(doctorService.getById(id));
    }
}
