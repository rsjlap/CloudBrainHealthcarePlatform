package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.dto.request.admin.SaveAdminDoctorRequest;
import com.neusoft.cloudbrain.dto.request.admin.UpdateAdminDoctorRequest;
import com.neusoft.cloudbrain.dto.response.admin.AdminDoctorListVO;
import com.neusoft.cloudbrain.service.AdminDoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员-医生管理")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/admin/doctors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorController {

    private final AdminDoctorService adminDoctorService;

    @Operation(summary = "医生列表（分页）")
    @GetMapping
    public Result<PageResult<AdminDoctorListVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(adminDoctorService.search(keyword, departmentId, status, page, size));
    }

    @Operation(summary = "医生详情")
    @GetMapping("/{id}")
    public Result<AdminDoctorListVO> detail(@PathVariable Long id) {
        return Result.success(adminDoctorService.getDetail(id));
    }

    @Operation(summary = "创建医生账号")
    @PostMapping
    public Result<AdminDoctorListVO> create(@Valid @RequestBody SaveAdminDoctorRequest request) {
        return Result.success(adminDoctorService.create(request));
    }

    @Operation(summary = "编辑医生")
    @PutMapping("/{id}")
    public Result<AdminDoctorListVO> update(@PathVariable Long id,
                                            @RequestBody UpdateAdminDoctorRequest request) {
        return Result.success(adminDoctorService.update(id, request));
    }
}
