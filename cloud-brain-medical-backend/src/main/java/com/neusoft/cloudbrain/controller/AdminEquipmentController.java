package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.enums.EquipmentStatus;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.dto.request.equipment.CreateEquipmentUsageRequest;
import com.neusoft.cloudbrain.dto.request.equipment.SaveMedicalEquipmentRequest;
import com.neusoft.cloudbrain.dto.request.equipment.UpdateEquipmentStatusRequest;
import com.neusoft.cloudbrain.dto.response.equipment.EquipmentStatsVO;
import com.neusoft.cloudbrain.dto.response.equipment.EquipmentUsageRecordVO;
import com.neusoft.cloudbrain.dto.response.equipment.MedicalEquipmentVO;
import com.neusoft.cloudbrain.service.AdminEquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理员-医疗设备")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/admin/equipment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEquipmentController {

    private final AdminEquipmentService adminEquipmentService;

    @Operation(summary = "设备状态统计")
    @GetMapping("/stats")
    public Result<EquipmentStatsVO> stats() {
        return Result.success(adminEquipmentService.getStats());
    }

    @Operation(summary = "设备分类名称列表（去重，来自已有设备）")
    @GetMapping("/category-names")
    public Result<List<String>> categoryNames() {
        return Result.success(adminEquipmentService.listCategoryNames());
    }

    @Operation(summary = "设备列表（分页）")
    @GetMapping
    public Result<PageResult<MedicalEquipmentVO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(adminEquipmentService.listEquipment(category, status, keyword, page, size));
    }

    @Operation(summary = "新增设备")
    @PostMapping
    public Result<MedicalEquipmentVO> create(@Valid @RequestBody SaveMedicalEquipmentRequest request) {
        return Result.success(adminEquipmentService.createEquipment(request));
    }

    @Operation(summary = "修改设备")
    @PutMapping("/{id}")
    public Result<MedicalEquipmentVO> update(@PathVariable Long id,
                                             @Valid @RequestBody SaveMedicalEquipmentRequest request) {
        return Result.success(adminEquipmentService.updateEquipment(id, request));
    }

    @Operation(summary = "更新设备状态（启用/停用/维护/故障）")
    @PutMapping("/{id}/status")
    public Result<MedicalEquipmentVO> updateStatus(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateEquipmentStatusRequest request) {
        return Result.success(adminEquipmentService.updateStatus(id, request));
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminEquipmentService.deleteEquipment(id);
        return Result.success();
    }

    @Operation(summary = "设备使用记录")
    @GetMapping("/{id}/usage-records")
    public Result<List<EquipmentUsageRecordVO>> usageRecords(@PathVariable Long id) {
        return Result.success(adminEquipmentService.listUsageRecords(id));
    }

    @Operation(summary = "登记设备使用")
    @PostMapping("/{id}/usage-records")
    public Result<EquipmentUsageRecordVO> createUsageRecord(@PathVariable Long id,
                                                             @Valid @RequestBody CreateEquipmentUsageRequest request) {
        return Result.success(adminEquipmentService.createUsageRecord(id, request));
    }
}
