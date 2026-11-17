package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.common.util.SecurityContextUtil;
import com.neusoft.cloudbrain.dto.request.medical.SaveMedicalRecordRequest;
import com.neusoft.cloudbrain.dto.response.medical.MedicalRecordVO;
import com.neusoft.cloudbrain.service.MedicalRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "病历模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @Operation(summary = "保存病历（医生）")
    @PostMapping
    public Result<MedicalRecordVO> save(@Valid @RequestBody SaveMedicalRecordRequest request) {
        return Result.success(medicalRecordService.save(SecurityContextUtil.getCurrentUserId(), request));
    }

    @Operation(summary = "确认病历（医生）")
    @PutMapping("/{id}/confirm")
    public Result<MedicalRecordVO> confirm(@PathVariable Long id) {
        return Result.success(medicalRecordService.confirm(id, SecurityContextUtil.getCurrentUserId()));
    }

    @Operation(summary = "我的接诊病历（医生）")
    @GetMapping("/doctor/my")
    public Result<List<MedicalRecordVO>> doctorRecords(
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(medicalRecordService.listByDoctor(
                SecurityContextUtil.getCurrentUserId(), patientName, date));
    }

    @Operation(summary = "按挂号查询病历（医生）")
    @GetMapping("/by-registration/{registrationId}")
    public Result<MedicalRecordVO> getByRegistration(@PathVariable Long registrationId) {
        return Result.success(medicalRecordService.getByRegistration(
                registrationId, SecurityContextUtil.getCurrentUserId()));
    }

    @Operation(summary = "病历详情")
    @GetMapping("/{id}")
    public Result<MedicalRecordVO> getById(@PathVariable Long id) {
        return Result.success(medicalRecordService.getById(id));
    }
}
