package com.neusoft.cloudbrain.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.common.enums.ExamOrderType;
import com.neusoft.cloudbrain.dto.response.medical.RecommendedExamVO;
import com.neusoft.cloudbrain.service.ExamCatalogService;
import com.neusoft.cloudbrain.service.kg.KgDiseaseResolver;
import com.neusoft.cloudbrain.service.kg.KnowledgeGraphClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiExamRecommendService AI 检查推�?)
class AiExamRecommendServiceTest {

    @Mock
    private AiPromptService aiPromptService;
    @Mock
    private AiLlmClient aiLlmClient;
    @Mock
    private KnowledgeGraphClient knowledgeGraphClient;
    @Mock
    private ExamCatalogService examCatalogService;

    private AiExamRecommendService aiExamRecommendService;

    @BeforeEach
    void setUp() {
        aiExamRecommendService = new AiExamRecommendService(
                aiPromptService, aiLlmClient, knowledgeGraphClient,
                new KgDiseaseResolver(), examCatalogService, new ObjectMapper());
        when(aiLlmClient.isAvailable()).thenReturn(false);
    }

    @Test
    @DisplayName("正常：目录规则推荐检�?)
    void recommendExams_catalogFallback() {
        when(examCatalogService.buildCatalogPromptText()).thenReturn("检查目�?);
        RecommendedExamVO exam = RecommendedExamVO.builder()
                .orderType(ExamOrderType.LAB)
                .examType("血常规")
                .catalogMatched(true)
                .build();
        when(examCatalogService.recommendFromContext(anyString(), anyInt())).thenReturn(List.of(exam));
        when(examCatalogService.retainCatalogMatched(any())).thenReturn(List.of(exam));

        var result = aiExamRecommendService.recommendExams("发热", "咳嗽", "上呼吸道感染");

        assertFalse(result.isAiAvailable());
        assertEquals(1, result.getRecommendedExams().size());
        assertEquals("血常规", result.getRecommendedExams().get(0).getExamType());
    }

    @Test
    @DisplayName("边界：无匹配项目时返回提�?)
    void recommendExams_emptyCatalog() {
        when(examCatalogService.buildCatalogPromptText()).thenReturn("当前无启用的检�?检查项�?);
        when(examCatalogService.recommendFromContext(anyString(), anyInt())).thenReturn(List.of());
        when(examCatalogService.retainCatalogMatched(any())).thenReturn(List.of());

        var result = aiExamRecommendService.recommendExams("头痛", "", "待查");

        assertTrue(result.getRecommendedExams().isEmpty());
        assertTrue(result.getSummary().contains("暂无启用"));
    }
}
