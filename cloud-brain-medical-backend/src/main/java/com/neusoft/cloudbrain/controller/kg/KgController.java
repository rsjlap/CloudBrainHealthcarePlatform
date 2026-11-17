package com.neusoft.cloudbrain.controller.kg;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.dto.request.kg.KgChatRequest;
import com.neusoft.cloudbrain.dto.response.kg.KgHealthVO;
import com.neusoft.cloudbrain.service.kg.KnowledgeGraphClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "知识图谱模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/kg")
@RequiredArgsConstructor
public class KgController {

    private final KnowledgeGraphClient knowledgeGraphClient;

    @Operation(summary = "知识图谱服务探活", description = "检测远程 KG 服务是否可达")
    @GetMapping("/health")
    public Result<KgHealthVO> health() {
        return Result.success(KgHealthVO.builder()
                .enabled(knowledgeGraphClient.isEnabled())
                .healthy(knowledgeGraphClient.isHealthy())
                .baseUrl(knowledgeGraphClient.getBaseUrl())
                .apiMode(knowledgeGraphClient.getApiMode())
                .message(knowledgeGraphClient.isHealthy()
                        ? "连接正常" : knowledgeGraphClient.getLastError())
                .build());
    }

    @Operation(summary = "知识图谱通用问答")
    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@Valid @RequestBody KgChatRequest request) {
        String answer = knowledgeGraphClient.chat(request.getQuestion()).orElse("知识图谱暂无返回，请检查远程服务地址与网络");
        return Result.success(Map.of("answer", answer));
    }

    @Operation(summary = "查询疾病标准检查项")
    @PostMapping("/query/exam")
    public Result<Map<String, String>> queryExam(@RequestParam String disease) {
        String result = knowledgeGraphClient.queryExam(disease)
                .orElse("知识图谱暂无返回，请确认 KG 服务已启动且 base-url 指向正确地址");
        return Result.success(Map.of("disease", disease, "result", result));
    }
}
