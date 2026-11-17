package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.dto.request.admin.AdminAiChatRequest;
import com.neusoft.cloudbrain.dto.response.admin.AdminAiChatResponseVO;
import com.neusoft.cloudbrain.service.ai.AdminAiQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员-AI 查询")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/admin/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiController {

    private final AdminAiQueryService adminAiQueryService;

    @Operation(summary = "AI 智能查询", description = "基于 DeepSeek 理解问题，从系统数据库查询后作答，不访问外网")
    @PostMapping("/chat")
    public Result<AdminAiChatResponseVO> chat(@Valid @RequestBody AdminAiChatRequest request) {
        return Result.success(adminAiQueryService.chat(request.getQuestion()));
    }
}
