package com.neusoft.cloudbrain.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.common.constant.AiPromptConstants;
import com.neusoft.cloudbrain.common.enums.RiskLevel;
import com.neusoft.cloudbrain.dto.request.prescription.PrescriptionItemRequest;
import com.neusoft.cloudbrain.dto.response.ai.DrugInteractionVO;
import com.neusoft.cloudbrain.dto.response.ai.PrescriptionReviewVO;
import com.neusoft.cloudbrain.dto.response.ai.PrescriptionSuggestVO;
import com.neusoft.cloudbrain.entity.Drug;
import com.neusoft.cloudbrain.entity.Patient;
import com.neusoft.cloudbrain.mapper.PatientMapper;
import com.neusoft.cloudbrain.service.DrugCatalogService;
import com.neusoft.cloudbrain.service.kg.KnowledgeGraphClient;
import com.neusoft.cloudbrain.service.kg.KgDiseaseResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPrescriptionService {

    private static final int SUGGEST_LIMIT = 5;
    private static final Set<String> PENICILLIN_KEYWORDS = Set.of("青霉素", "阿莫西林", "氨苄西林");
    private static final Set<String> ASPIRIN_KEYWORDS = Set.of("阿司匹林");
    private static final Set<String> NITRO_KEYWORDS = Set.of("硝酸甘油");

    private final PatientMapper patientMapper;
    private final DrugCatalogService drugCatalogService;
    private final AiPromptService aiPromptService;
    private final AiLlmClient aiLlmClient;
    private final KnowledgeGraphClient knowledgeGraphClient;
    private final KgDiseaseResolver kgDiseaseResolver;
    private final ObjectMapper objectMapper;

    public PrescriptionSuggestVO suggestPrescription(Long patientId,
                                                     String chiefComplaint,
                                                     String symptoms,
                                                     String diagnosis) {
        Patient patient = patientMapper.selectById(patientId);
        String allergyHistory = patient != null && patient.getAllergyHistory() != null
                ? patient.getAllergyHistory() : "无";
        String catalog = drugCatalogService.buildCatalogPromptText();

        if (aiLlmClient.isAvailable()) {
            try {
                PrescriptionSuggestVO result = suggestWithLlm(
                        chiefComplaint, symptoms, diagnosis, allergyHistory, catalog);
                result.setAiAvailable(true);
                log.info("AI prescription suggest via LLM patientId={}", patientId);
                return finalizeSuggest(result, catalog, allergyHistory, chiefComplaint, symptoms, diagnosis);
            } catch (Exception ex) {
                log.warn("LLM prescription suggest failed patientId={}", patientId, ex);
            }
        }
        return finalizeSuggest(suggestFromCatalog(chiefComplaint, symptoms, diagnosis, allergyHistory),
                catalog, allergyHistory, chiefComplaint, symptoms, diagnosis);
    }

    private PrescriptionSuggestVO finalizeSuggest(PrescriptionSuggestVO result,
                                                  String catalog,
                                                  String allergyHistory,
                                                  String chiefComplaint,
                                                  String symptoms,
                                                  String diagnosis) {
        List<PrescriptionItemRequest> matched = drugCatalogService.retainCatalogMatched(
                result.getRecommendedItems() != null ? result.getRecommendedItems() : List.of());
        result.setRecommendedItems(matched);
        if (matched.isEmpty()) {
            if (catalog.contains("当前无在售药品")) {
                result.setOverallAdvice("暂无在售药品目录，请先在管理端维护药品");
            } else if (!StringUtils.hasText(result.getOverallAdvice())) {
                result.setOverallAdvice("未从药品目录中匹配到合适药品，请手动搜索添加");
            }
        }
        return result;
    }

    private PrescriptionSuggestVO suggestFromCatalog(String chiefComplaint,
                                                     String symptoms,
                                                     String diagnosis,
                                                     String allergyHistory) {
        String context = defaultText(chiefComplaint) + " " + defaultText(symptoms) + " " + defaultText(diagnosis);
        List<PrescriptionItemRequest> items = drugCatalogService.recommendFromContext(
                context, allergyHistory, SUGGEST_LIMIT);
        return PrescriptionSuggestVO.builder()
                .riskLevel(RiskLevel.LOW)
                .overallAdvice(items.isEmpty()
                        ? "未从药品目录中匹配到合适药品"
                        : "根据症状与药品目录规则推荐")
                .warnings(List.of())
                .recommendedItems(items)
                .interactions(List.of())
                .aiAvailable(false)
                .build();
    }

    public PrescriptionReviewVO reviewPrescription(Long patientId,
                                                     String diagnosis,
                                                     List<PrescriptionItemRequest> items) {
        Patient patient = patientMapper.selectById(patientId);
        String allergyHistory = patient != null && patient.getAllergyHistory() != null
                ? patient.getAllergyHistory() : "";
        drugCatalogService.enrichRecommendedItems(items);

        if (aiLlmClient.isAvailable()) {
            try {
                PrescriptionReviewVO draft = reviewWithLlm(allergyHistory, diagnosis, items);
                PrescriptionReviewVO review = mergeReviewWithKg(draft, allergyHistory, diagnosis, items);
                review.setInventoryAdvice(checkInventory(items));
                review.setAiAvailable(true);
                log.info("AI prescription review via LLM+KG patientId={}", patientId);
                return review;
            } catch (Exception ex) {
                log.warn("LLM prescription review failed, fallback to rules patientId={}", patientId, ex);
            }
        }

        PrescriptionReviewVO review = reviewWithRules(patientId, items, allergyHistory);
        review.setInventoryAdvice(checkInventory(items));
        return review;
    }

    private PrescriptionReviewVO mergeReviewWithKg(PrescriptionReviewVO draft,
                                                     String allergyHistory,
                                                     String diagnosis,
                                                     List<PrescriptionItemRequest> items) {
        String disease = kgDiseaseResolver.resolve(diagnosis);
        if (!StringUtils.hasText(disease)) {
            draft.setKgAvailable(false);
            return draft;
        }

        Optional<String> kgDrugResult = knowledgeGraphClient.queryDrug(disease);
        Optional<String> kgForbiddenResult = knowledgeGraphClient.queryDrugForbidden(disease);
        if (kgDrugResult.isEmpty() && kgForbiddenResult.isEmpty()) {
            draft.setKgAvailable(false);
            return draft;
        }

        draft.setKgAvailable(true);
        draft.setKgReference(truncate(joinKgReferences(kgDrugResult.orElse(""), kgForbiddenResult.orElse(""))));
        try {
            String prompt = aiPromptService.render(AiPromptConstants.PRESCRIPTION_REVIEW_KG_MERGE, Map.of(
                    "allergyHistory", defaultText(allergyHistory),
                    "diagnosis", defaultText(disease),
                    "drugList", serializeDrugList(items),
                    "aiReview", toJson(draft),
                    "kgDrugResult", kgDrugResult.orElse("知识图谱暂无药品数据"),
                    "kgForbiddenResult", kgForbiddenResult.orElse("知识图谱暂无禁忌数据"),
                    "catalog", drugCatalogService.buildCatalogPromptText()
            ));
            PrescriptionReviewVO merged = aiLlmClient.chatForJson(prompt, PrescriptionReviewVO.class);
            merged.setKgAvailable(true);
            merged.setKgReference(draft.getKgReference());
            return merged;
        } catch (Exception ex) {
            log.warn("KG prescription review merge failed disease={}", disease, ex);
            return draft;
        }
    }

    private PrescriptionSuggestVO suggestWithLlm(String chiefComplaint,
                                                 String symptoms,
                                                 String diagnosis,
                                                 String allergyHistory,
                                                 String catalog) {
        String prompt = aiPromptService.render(AiPromptConstants.PRESCRIPTION_SUGGEST, Map.of(
                "chiefComplaint", defaultText(chiefComplaint),
                "symptoms", defaultText(symptoms),
                "diagnosis", defaultText(diagnosis),
                "allergyHistory", defaultText(allergyHistory),
                "catalog", catalog
        ));
        return aiLlmClient.chatForJson(prompt, PrescriptionSuggestVO.class);
    }

    private PrescriptionReviewVO reviewWithLlm(String allergyHistory,
                                               String diagnosis,
                                               List<PrescriptionItemRequest> items) {
        String prompt = aiPromptService.render(AiPromptConstants.PRESCRIPTION_REVIEW, Map.of(
                "allergyHistory", defaultText(allergyHistory),
                "diagnosis", defaultText(diagnosis),
                "drugList", serializeDrugList(items),
                "catalog", drugCatalogService.buildCatalogPromptText()
        ));
        return aiLlmClient.chatForJson(prompt, PrescriptionReviewVO.class);
    }

    private PrescriptionReviewVO reviewWithRules(Long patientId,
                                                 List<PrescriptionItemRequest> items,
                                                 String allergyHistory) {
        List<DrugInteractionVO> interactions = new ArrayList<>();
        RiskLevel maxRisk = RiskLevel.LOW;

        if (StringUtils.hasText(allergyHistory) && allergyHistory.contains("青霉素")) {
            for (PrescriptionItemRequest item : items) {
                if (matchesKeywords(item, PENICILLIN_KEYWORDS)) {
                    interactions.add(DrugInteractionVO.builder()
                            .drug1(item.getDrugName())
                            .drug2("过敏史：" + allergyHistory)
                            .description("患者存在青霉素过敏史，禁止开具青霉素类药品")
                            .severity("HIGH")
                            .build());
                    maxRisk = RiskLevel.HIGH;
                }
            }
        }

        boolean hasAspirin = items.stream().anyMatch(item -> matchesKeywords(item, ASPIRIN_KEYWORDS));
        boolean hasNitro = items.stream().anyMatch(item -> matchesKeywords(item, NITRO_KEYWORDS));
        if (hasAspirin && hasNitro) {
            interactions.add(DrugInteractionVO.builder()
                    .drug1("阿司匹林")
                    .drug2("硝酸甘油")
                    .description("联用可能增强血管扩张与出血风险，需密切监测血压")
                    .severity("MEDIUM")
                    .build());
            if (maxRisk != RiskLevel.HIGH) {
                maxRisk = RiskLevel.MEDIUM;
            }
        }

        Set<String> seen = new HashSet<>();
        for (PrescriptionItemRequest item : items) {
            String key = item.getDrugId() != null
                    ? "id:" + item.getDrugId()
                    : (StringUtils.hasText(item.getDrugCode()) ? item.getDrugCode() : item.getDrugName());
            if (!seen.add(key)) {
                interactions.add(DrugInteractionVO.builder()
                        .drug1(item.getDrugName())
                        .drug2(item.getDrugName())
                        .description("处方中存在重复药品，请确认是否必要")
                        .severity("MEDIUM")
                        .build());
                if (maxRisk == RiskLevel.LOW) {
                    maxRisk = RiskLevel.MEDIUM;
                }
            }
        }

        String overallAdvice = buildAdvice(maxRisk, interactions.size());
        log.info("Prescription review patientId={} risk={} interactions={}", patientId, maxRisk, interactions.size());

        return PrescriptionReviewVO.builder()
                .riskLevel(maxRisk)
                .overallAdvice(overallAdvice)
                .warnings(List.of())
                .interactions(interactions)
                .aiAvailable(false)
                .kgAvailable(false)
                .build();
    }

    private String serializeDrugList(List<PrescriptionItemRequest> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException ex) {
            return items.stream()
                    .map(i -> i.getDrugName() + " " + i.getDosage() + i.getUnit() + " " + i.getFrequency())
                    .collect(Collectors.joining("\n"));
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private String joinKgReferences(String drugResult, String forbiddenResult) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(drugResult)) {
            sb.append("【常用药品】").append(drugResult.trim());
        }
        if (StringUtils.hasText(forbiddenResult)) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append("【禁忌信息】").append(forbiddenResult.trim());
        }
        return sb.toString();
    }

    private String truncate(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500) + "...";
    }

    private String checkInventory(List<PrescriptionItemRequest> items) {
        List<String> warnings = new ArrayList<>();
        for (PrescriptionItemRequest item : items) {
            Drug drug = null;
            if (item.getDrugId() != null) {
                drug = drugCatalogService.getActiveDrug(item.getDrugId());
            } else if (StringUtils.hasText(item.getDrugCode())) {
                drug = drugCatalogService.getActiveDrugByCode(item.getDrugCode());
            }
            if (drug == null) {
                warnings.add(item.getDrugName() + "：药品不在目录或已下架");
            } else if (drug.getStockQuantity() == null || drug.getStockQuantity() <= 0) {
                warnings.add(item.getDrugName() + "：库存不足");
            }
        }
        if (warnings.isEmpty()) {
            return "药房库存充足，可以开具。";
        }
        return String.join("；", warnings);
    }

    private boolean matchesKeywords(PrescriptionItemRequest item, Set<String> keywords) {
        String name = item.getDrugName() != null ? item.getDrugName() : "";
        String code = item.getDrugCode() != null ? item.getDrugCode() : "";
        for (String keyword : keywords) {
            if (name.contains(keyword) || code.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String buildAdvice(RiskLevel risk, int interactionCount) {
        return switch (risk) {
            case HIGH -> "存在高风险用药问题，请修改处方后再提交。";
            case MEDIUM -> interactionCount > 0
                    ? "存在中等风险，请医生结合患者情况确认后开具。"
                    : "未发现明显用药风险，请医生最终确认。";
            case LOW -> "未发现明显用药风险，请医生最终确认。";
        };
    }

    private String defaultText(String text) {
        return StringUtils.hasText(text) ? text.trim() : "未提供";
    }
}
