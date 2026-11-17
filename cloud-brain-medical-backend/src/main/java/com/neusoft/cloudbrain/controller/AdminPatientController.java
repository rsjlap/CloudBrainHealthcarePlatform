package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.dto.request.patient.UpdateAdminPatientRequest;
import com.neusoft.cloudbrain.dto.response.patient.AdminPatientDetailVO;
import com.neusoft.cloudbrain.dto.response.patient.AdminPatientListVO;
import com.neusoft.cloudbrain.dto.response.patient.AdminPatientStatsVO;
import com.neusoft.cloudbrain.service.AdminPatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import com.neusoft.cloudbrain.common.result.PageResult;

@Tag(name = "管理员-患者管理")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/admin/patients")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPatientController {

    private final AdminPatientService adminPatientService;

    @Operation(summary = "患者统计")
    @GetMapping("/stats")
    public Result<AdminPatientStatsVO> stats() {
        return Result.success(adminPatientService.getStats());
    }

    @Operation(summary = "患者列表（多条件查询、分页）")
    @GetMapping
    public Result<PageResult<AdminPatientListVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) Boolean hasAllergy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registerDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registerDateTo,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(adminPatientService.search(
                keyword, gender, hasAllergy, registerDateFrom, registerDateTo, page, size));
    }

    @Operation(summary = "患者详情（档案/挂号/病历/健康档案）")
    @GetMapping("/{id}")
    public Result<AdminPatientDetailVO> detail(@PathVariable Long id) {
        return Result.success(adminPatientService.getDetail(id));
    }

    @Operation(summary = "更新患者档案")
    @PutMapping("/{id}")
    public Result<AdminPatientDetailVO> update(@PathVariable Long id,
                                               @RequestBody UpdateAdminPatientRequest request) {
        return Result.success(adminPatientService.update(id, request));
    }
}
