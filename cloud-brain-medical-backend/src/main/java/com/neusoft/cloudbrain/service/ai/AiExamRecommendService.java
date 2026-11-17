package com.neusoft.cloudbrain.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.common.constant.AiPromptConstants;
import com.neusoft.cloudbrain.dto.response.ai.ExamRecommendVO;
import com.neusoft.cloudbrain.dto.response.medical.RecommendedExamVO;
import com.neusoft.cloudbrain.service.ExamCatalogService;
import com.neusoft.cloudbrain.service.kg.KnowledgeGraphClient;
import com.neusoft.cloudbrain.service.kg.KgDiseaseResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiExamRecommendService {

    private static final int RECOMMEND_LIMIT = 4;

    private final AiPromptService aiPromptService;
    private final AiLlmClient aiLlmClient;
    private final KnowledgeGraphClient knowledgeGraphClient;
    private final KgDiseaseResolver kgDiseaseResolver;
    private final ExamCatalogService examCatalogService;
    private final ObjectMapper objectMapper;

    public ExamRecommendVO recommendExams(String chiefComplaint, String symptoms, String diagnosis) {
        String catalog = examCatalogService.buildCatalogPromptText();
        if (aiLlmClient.isAvailable()) {
            try {
                ExamRecommendVO draft = recommendWithLlm(chiefComplaint, symptoms, diagnosis, catalog);
                ExamRecommendVO result = mergeExamWithKg(draft, chiefComplaint, symptoms, diagnosis, catalog);
                log.info("AI exam recommendation via LLM+KG count={}",
                        result.getRecommendedExams() != null ? result.getRecommendedExams().size() : 0);
                return finalizeResult(result, catalog);
            } catch (Exception ex) {
                log.warn("LLM exam recommendation failed, fallback to catalog rules", ex);
            }
        }
        return finalizeResult(recommendFromCatalog(chiefComplaint, symptoms, diagnosis), catalog);
    }

    private ExamRecommendVO finalizeResult(ExamRecommendVO result, String catalog) {
        List<RecommendedExamVO> matched = examCatalogService.retainCatalogMatched(
                result.getRecommendedExams() != null ? result.getRecommendedExams() : List.of());
        result.setRecommendedExams(matched);
        if (matched.isEmpty() && catalog.contains("当前无启用")) {
            result.setSummary("暂无启用的检验/检查项目目录，请先在管理端维护项目");
        } else if (matched.isEmpty() && !StringUtils.hasText(result.getSummary())) {
            result.setSummary("未从医院项目目录中匹配到合适项目，请手动搜索添加");
        }
        return result;
    }

    private ExamRecommendVO recommendFromCatalog(String chiefComplaint, String symptoms, String diagnosis) {
        String context = defaultText(chiefComplaint) + " " + defaultText(symptoms) + " " + defaultText(diagnosis);
        List<RecommendedExamVO> exams = examCatalogService.recommendFromContext(context, RECOMMEND_LIMIT);
        return ExamRecommendVO.builder()
                .recommendedExams(exams)
                .summary(exams.isEmpty()
                        ? "未从医院项目目录中匹配到合适项目"
                        : "根据症状与医院项目目录规则推荐")
                .aiAvailable(false)
                .kgAvailable(false)
                .build();
    }

    private ExamRecommendVO mergeExamWithKg(ExamRecommendVO draft,
                                            String chiefComplaint,
                                            String symptoms,
                                            String diagnosis,
                                            String catalog) {
        draft.setAiAvailable(true);
        String disease = kgDiseaseResolver.resolve(diagnosis, draft.getSummary(), chiefComplaint, symptoms);
        if (!StringUtils.hasText(disease)) {
            draft.setKgAvailable(false);
            return draft;
        }

        Optional<String> kgExamResult = knowledgeGraphClient.queryExam(disease);
        if (kgExamResult.isEmpty()) {
            draft.setKgAvailable(false);
            return draft;
        }

        draft.setKgAvailable(true);
        draft.setKgReference(truncate(kgExamResult.get()));
        try {
            String prompt = aiPromptService.render(AiPromptConstants.EXAM_RECOMMENDATION_KG_MERGE, Map.of(
                    "chiefComplaint", defaultText(chiefComplaint),
                    "symptoms", defaultText(symptoms),
                    "diagnosis", defaultText(diagnosis),
                    "disease", disease,
                    "aiDraft", toJson(draft),
                    "kgExamResult", kgExamResult.get(),
                    "catalog", catalog
            ));
            ExamRecommendVO merged = aiLlmClient.chatForJson(prompt, ExamRecommendVO.class);
            merged.setAiAvailable(true);
            merged.setKgAvailable(true);
            merged.setKgReference(truncate(kgExamResult.get()));
            return merged;
        } catch (Exception ex) {
            log.warn("KG exam merge failed, return AI draft with KG reference disease={}", disease, ex);
            if (!StringUtils.hasText(draft.getSummary())) {
                draft.setSummary("已结合知识图谱标准检查项，详见 kgReference");
            }
            return draft;
        }
    }

    private ExamRecommendVO recommendWithLlm(String chiefComplaint, String symptoms, String diagnosis, String catalog) {
        String prompt = aiPromptService.render(AiPromptConstants.EXAM_RECOMMENDATION, Map.of(
                "chiefComplaint", defaultText(chiefComplaint),
                "symptoms", defaultText(symptoms),
                "diagnosis", defaultText(diagnosis),
                "catalog", catalog
        ));
        ExamRecommendVO result = aiLlmClient.chatForJson(prompt, ExamRecommendVO.class);
        result.setAiAvailable(true);
        return result;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private String truncate(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500) + "...";
    }

    private String defaultText(String text) {
        return StringUtils.hasText(text) ? text.trim() : "未提供";
    }
}
