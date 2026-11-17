package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.common.util.SecurityContextUtil;
import com.neusoft.cloudbrain.dto.request.prescription.CreatePrescriptionRequest;
import com.neusoft.cloudbrain.dto.response.prescription.PrescriptionVO;
import com.neusoft.cloudbrain.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "处方模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @Operation(summary = "创建处方")
    @PostMapping
    public Result<PrescriptionVO> create(@Valid @RequestBody CreatePrescriptionRequest request) {
        return Result.success(prescriptionService.create(SecurityContextUtil.getCurrentUserId(), request));
    }

    @Operation(summary = "我的处方记录（医生）")
    @GetMapping("/my")
    public Result<List<PrescriptionVO>> myPrescriptions() {
        return Result.success(prescriptionService.listByDoctor(SecurityContextUtil.getCurrentUserId()));
    }

    @Operation(summary = "按挂号查询处方")
    @GetMapping("/registrations/{registrationId}")
    public Result<List<PrescriptionVO>> listByRegistration(@PathVariable Long registrationId) {
        return Result.success(prescriptionService.listByRegistration(registrationId));
    }

    @Operation(summary = "待发药队列（仅已缴费）")
    @GetMapping("/pending")
    @PreAuthorize("hasRole('PHARMACIST')")
    public Result<List<PrescriptionVO>> pending() {
        return Result.success(prescriptionService.listPendingForPharmacy());
    }

    @Operation(summary = "发药历史记录")
    @GetMapping("/pharmacy/history")
    public Result<List<PrescriptionVO>> dispensedHistory(
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(prescriptionService.listDispensedHistory(patientName, doctorName, date));
    }

    @Operation(summary = "处方详情")
    @GetMapping("/{id}")
    public Result<PrescriptionVO> getById(@PathVariable Long id) {
        return Result.success(prescriptionService.getById(id));
    }

    @Operation(summary = "发药确认")
    @PutMapping("/{id}/dispense")
    @PreAuthorize("hasRole('PHARMACIST')")
    public Result<PrescriptionVO> dispense(@PathVariable Long id) {
        return Result.success(prescriptionService.dispense(id, SecurityContextUtil.getCurrentUserId()));
    }
}
