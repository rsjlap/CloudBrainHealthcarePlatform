package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.dto.request.admin.SaveAdminDrugRequest;
import com.neusoft.cloudbrain.dto.request.admin.UpdateAdminDrugRequest;
import com.neusoft.cloudbrain.dto.response.admin.AdminDrugVO;
import com.neusoft.cloudbrain.service.AdminDrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员-药品管理")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/admin/drugs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDrugController {

    private final AdminDrugService adminDrugService;

    @Operation(summary = "药品列表（分页）")
    @GetMapping
    public Result<PageResult<AdminDrugVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(adminDrugService.search(keyword, status, page, size));
    }

    @Operation(summary = "药品详情")
    @GetMapping("/{id}")
    public Result<AdminDrugVO> detail(@PathVariable Long id) {
        return Result.success(adminDrugService.getDetail(id));
    }

    @Operation(summary = "新增药品")
    @PostMapping
    public Result<AdminDrugVO> create(@Valid @RequestBody SaveAdminDrugRequest request) {
        return Result.success(adminDrugService.create(request));
    }

    @Operation(summary = "编辑药品")
    @PutMapping("/{id}")
    public Result<AdminDrugVO> update(@PathVariable Long id, @RequestBody UpdateAdminDrugRequest request) {
        return Result.success(adminDrugService.update(id, request));
    }
}
