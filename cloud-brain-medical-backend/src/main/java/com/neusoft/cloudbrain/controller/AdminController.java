package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.enums.UserRole;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.dto.response.admin.AdminDashboardVO;
import com.neusoft.cloudbrain.dto.response.admin.AdminUserVO;
import com.neusoft.cloudbrain.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.dto.request.admin.UpdateAdminStaffRequest;

import java.util.List;

@Tag(name = "管理员模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "仪表盘统计")
    @GetMapping("/dashboard")
    public Result<AdminDashboardVO> dashboard() {
        return Result.success(adminService.getDashboardStats());
    }

    @Operation(summary = "用户列表（全部，兼容旧接口）")
    @GetMapping("/users")
    public Result<List<AdminUserVO>> users() {
        return Result.success(adminService.listUsers());
    }

    @Operation(summary = "工作人员列表（分页）")
    @GetMapping("/users/staff")
    public Result<PageResult<AdminUserVO>> staffUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(adminService.listStaff(role, keyword, page, size));
    }

    @Operation(summary = "更新工作人员（非医生角色）")
    @PutMapping("/users/staff/{id}")
    public Result<AdminUserVO> updateStaff(@PathVariable Long id,
                                         @RequestBody UpdateAdminStaffRequest request) {
        return Result.success(adminService.updateStaff(id, request));
    }
}
