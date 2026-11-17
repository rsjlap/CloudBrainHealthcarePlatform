package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.common.util.SecurityContextUtil;
import com.neusoft.cloudbrain.dto.response.payment.PatientBillVO;
import com.neusoft.cloudbrain.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "缴费模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "我的待缴/已缴账单（患者）")
    @GetMapping("/my-bills")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<List<PatientBillVO>> myBills() {
        return Result.success(paymentService.listMyBills(SecurityContextUtil.getCurrentUserId()));
    }

    @Operation(summary = "缴纳挂号费")
    @PostMapping("/registration/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<Void> payRegistration(@PathVariable Long id) {
        paymentService.payRegistration(SecurityContextUtil.getCurrentUserId(), id);
        return Result.success();
    }

    @Operation(summary = "缴纳检查/检验费")
    @PostMapping("/exam-order/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<Void> payExamOrder(@PathVariable Long id) {
        paymentService.payExamOrder(SecurityContextUtil.getCurrentUserId(), id);
        return Result.success();
    }

    @Operation(summary = "缴纳处方费")
    @PostMapping("/prescription/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<Void> payPrescription(@PathVariable Long id) {
        paymentService.payPrescription(SecurityContextUtil.getCurrentUserId(), id);
        return Result.success();
    }
}
