package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.config.SwaggerConfig;
import com.neusoft.cloudbrain.dto.response.catalog.ExamItemVO;
import com.neusoft.cloudbrain.dto.response.catalog.LabItemVO;
import com.neusoft.cloudbrain.service.ExamCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "检查检验目录")
@SecurityRequirement(name = SwaggerConfig.AUTH_SCHEME)
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/catalog")
@RequiredArgsConstructor
public class ExamCatalogController {

    private final ExamCatalogService examCatalogService;

    @Operation(summary = "检验项目目录（启用）")
    @GetMapping("/lab-items")
    public Result<List<LabItemVO>> listLabItems(@RequestParam(required = false) String keyword) {
        return Result.success(examCatalogService.listEnabledLabItems(keyword));
    }

    @Operation(summary = "检查项目目录（启用）")
    @GetMapping("/exam-items")
    public Result<List<ExamItemVO>> listExamItems(@RequestParam(required = false) String keyword) {
        return Result.success(examCatalogService.listEnabledExamItems(keyword));
    }
}
