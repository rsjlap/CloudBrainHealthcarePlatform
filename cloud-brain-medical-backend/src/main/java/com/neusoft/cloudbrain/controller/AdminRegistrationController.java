package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.dto.response.registration.RegistrationVO;
import com.neusoft.cloudbrain.service.AdminRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "管理员-挂号记录")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/admin/registrations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRegistrationController {

    private final AdminRegistrationService adminRegistrationService;

    @Operation(summary = "挂号记录列表（分页、多条件筛选）")
    @GetMapping
    public Result<PageResult<RegistrationVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(adminRegistrationService.search(
                keyword, status, departmentId, doctorId, paid, dateFrom, dateTo, page, size));
    }

    @Operation(summary = "挂号详情")
    @GetMapping("/{id}")
    public Result<RegistrationVO> detail(@PathVariable Long id) {
        return Result.success(adminRegistrationService.getDetail(id));
    }
}
