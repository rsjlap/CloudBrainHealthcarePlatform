package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "科室模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "科室列表")
    @GetMapping
    public Result<List<Department>> list() {
        return Result.success(departmentService.listAll());
    }

    @Operation(summary = "科室详情")
    @GetMapping("/{id}")
    public Result<Department> getById(@PathVariable Long id) {
        return Result.success(departmentService.getById(id));
    }
}
