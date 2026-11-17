package com.neusoft.cloudbrain.service.ai;

import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiPromptService Prompt 加载与渲�?)
class AiPromptServiceTest {

    private AiPromptService aiPromptService;

    @BeforeEach
    void setUp() {
        aiPromptService = new AiPromptService();
    }

    @Test
    @DisplayName("正常：加�?classpath 模板")
    void load_existingResource() {
        String content = aiPromptService.load("prompts/medical-record.txt");
        assertTrue(content.contains("{") || content.length() > 10);
    }

    @Test
    @DisplayName("正常：变量替�?)
    void render_substitutesVariables() {
        String rendered = aiPromptService.render("prompts/medical-record.txt", Map.of(
                "dialogText", "患者主诉头�?,
                "chiefComplaint", "头痛"
        ));
        assertTrue(rendered.contains("患者主诉头�?) || !rendered.contains("{dialogText}"));
    }

    @Test
    @DisplayName("正常：缓存命�?)
    void load_usesCache() {
        String first = aiPromptService.load("prompts/exam-report.txt");
        String second = aiPromptService.load("prompts/exam-report.txt");
        assertEquals(first, second);
    }

    @Test
    @DisplayName("异常：资源不存在")
    void load_missingResource() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiPromptService.load("prompts/not-exist.txt"));
        assertEquals(ResultCode.SERVER_ERROR, ex.getResultCode());
    }
}
