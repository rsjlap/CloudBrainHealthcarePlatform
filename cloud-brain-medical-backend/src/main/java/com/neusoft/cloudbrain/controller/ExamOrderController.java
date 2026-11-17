package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.common.util.SecurityContextUtil;
import com.neusoft.cloudbrain.dto.request.exam.CreateExamOrderRequest;
import com.neusoft.cloudbrain.dto.response.exam.ExamOrderVO;
import com.neusoft.cloudbrain.service.ExamOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "检查申请模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/exam-orders")
@RequiredArgsConstructor
public class ExamOrderController {

    private final ExamOrderService examOrderService;

    @Operation(summary = "开立检查申请", description = "请求体请选 raw/JSON；也兼容 x-www-form-urlencoded 表单提交")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<ExamOrderVO> create(@Valid @RequestBody CreateExamOrderRequest request) {
        return doCreate(request);
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<ExamOrderVO> createFromForm(@Valid @ModelAttribute CreateExamOrderRequest request) {
        return doCreate(request);
    }

    @Operation(summary = "待检申请队列")
    @GetMapping("/pending")
    public Result<List<ExamOrderVO>> pending() {
        return Result.success(examOrderService.listPendingForInspector());
    }

    @Operation(summary = "挂号关联检查申请")
    @GetMapping("/registrations/{registrationId}")
    public Result<List<ExamOrderVO>> listByRegistration(@PathVariable Long registrationId) {
        return Result.success(examOrderService.listByRegistration(registrationId));
    }

    private Result<ExamOrderVO> doCreate(CreateExamOrderRequest request) {
        return Result.success(examOrderService.create(SecurityContextUtil.getCurrentUserId(), request));
    }
}
