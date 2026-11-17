package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.common.util.SecurityContextUtil;
import com.neusoft.cloudbrain.dto.request.exam.SaveExamReportRequest;
import com.neusoft.cloudbrain.dto.response.exam.ExamReportVO;
import com.neusoft.cloudbrain.dto.response.exam.InspectorExamHistoryVO;
import com.neusoft.cloudbrain.service.ExamReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "检查报告模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/exam-reports")
@RequiredArgsConstructor
public class ExamReportController {

    private final ExamReportService examReportService;

    @Operation(summary = "一键生成并保存检查报告")
    @PostMapping("/generate/{orderId}")
    public Result<ExamReportVO> generate(@PathVariable Long orderId) {
        return Result.success(examReportService.generateAndSave(
                SecurityContextUtil.getCurrentUserId(), orderId));
    }

    @Operation(summary = "提交检查报告")
    @PostMapping
    public Result<ExamReportVO> save(@Valid @RequestBody SaveExamReportRequest request) {
        return Result.success(examReportService.save(SecurityContextUtil.getCurrentUserId(), request));
    }

    @Operation(summary = "确认发布报告")
    @PutMapping("/{id}/confirm")
    public Result<ExamReportVO> confirm(@PathVariable Long id) {
        return Result.success(examReportService.confirm(id));
    }

    @Operation(summary = "按申请单查询报告")
    @GetMapping("/orders/{orderId}")
    public Result<ExamReportVO> getByOrder(@PathVariable Long orderId) {
        return Result.success(examReportService.getByOrderId(orderId));
    }

    @Operation(summary = "检查医师历史检查记录")
    @GetMapping("/inspector/history")
    public Result<List<InspectorExamHistoryVO>> inspectorHistory(
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(examReportService.listHistoryForInspector(patientName, doctorName, date));
    }
}
