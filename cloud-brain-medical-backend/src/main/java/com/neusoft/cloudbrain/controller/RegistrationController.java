package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.common.util.SecurityContextUtil;
import com.neusoft.cloudbrain.dto.request.registration.CreateRegistrationRequest;
import com.neusoft.cloudbrain.dto.response.registration.RegistrationPatientInfoVO;
import com.neusoft.cloudbrain.dto.response.registration.RegistrationVO;
import com.neusoft.cloudbrain.config.SwaggerConfig;
import com.neusoft.cloudbrain.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "挂号模块")
@SecurityRequirement(name = SwaggerConfig.AUTH_SCHEME)
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @Operation(summary = "患者挂号")
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public Result<RegistrationVO> create(@Valid @RequestBody CreateRegistrationRequest request) {
        return Result.success(registrationService.create(SecurityContextUtil.getCurrentUserId(), request));
    }

    @Operation(summary = "我的挂号记录（患者）")
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<List<RegistrationVO>> myRegistrations() {
        return Result.success(registrationService.listByPatient(SecurityContextUtil.getCurrentUserId()));
    }

    @Operation(summary = "今日患者队列（医生）")
    @GetMapping("/queue")
    public Result<List<RegistrationVO>> queue(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate workDate = date != null ? date : LocalDate.now();
        return Result.success(registrationService.listQueueByDoctor(SecurityContextUtil.getCurrentUserId(), workDate));
    }

    @Operation(summary = "挂号关联患者信息（医生）")
    @GetMapping("/{id}/patient-info")
    public Result<RegistrationPatientInfoVO> patientInfo(@PathVariable Long id) {
        return Result.success(registrationService.getPatientInfo(id, SecurityContextUtil.getCurrentUserId()));
    }

    @Operation(summary = "叫号")
    @PutMapping("/{id}/call")
    public Result<RegistrationVO> call(@PathVariable Long id) {
        return Result.success(registrationService.callNext(id, SecurityContextUtil.getCurrentUserId()));
    }

    @Operation(summary = "完成就诊")
    @PutMapping("/{id}/complete")
    public Result<RegistrationVO> complete(@PathVariable Long id) {
        return Result.success(registrationService.complete(id, SecurityContextUtil.getCurrentUserId()));
    }
}
