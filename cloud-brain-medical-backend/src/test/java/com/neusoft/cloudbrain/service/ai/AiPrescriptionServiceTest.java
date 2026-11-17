package com.neusoft.cloudbrain.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.common.enums.RiskLevel;
import com.neusoft.cloudbrain.dto.request.prescription.PrescriptionItemRequest;
import com.neusoft.cloudbrain.entity.Drug;
import com.neusoft.cloudbrain.entity.Patient;
import com.neusoft.cloudbrain.mapper.PatientMapper;
import com.neusoft.cloudbrain.service.DrugCatalogService;
import com.neusoft.cloudbrain.service.kg.KgDiseaseResolver;
import com.neusoft.cloudbrain.service.kg.KnowledgeGraphClient;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiPrescriptionService AI 处方建议与审�?)
class AiPrescriptionServiceTest {

    @Mock
    private PatientMapper patientMapper;
    @Mock
    private DrugCatalogService drugCatalogService;
    @Mock
    private AiPromptService aiPromptService;
    @Mock
    private AiLlmClient aiLlmClient;
    @Mock
    private KnowledgeGraphClient knowledgeGraphClient;

    private AiPrescriptionService aiPrescriptionService;

    @BeforeEach
    void setUp() {
        KgDiseaseResolver kgDiseaseResolver = new KgDiseaseResolver();
        aiPrescriptionService = new AiPrescriptionService(
                patientMapper, drugCatalogService, aiPromptService, aiLlmClient,
                knowledgeGraphClient, kgDiseaseResolver, new ObjectMapper());
        when(aiLlmClient.isAvailable()).thenReturn(false);
    }

    @Test
    @DisplayName("正常：目录规则推荐处�?)
    void suggestPrescription_catalogFallback() {
        Patient patient = new Patient();
        patient.setAllergyHistory("�?);
        when(patientMapper.selectById(1L)).thenReturn(patient);
        when(drugCatalogService.buildCatalogPromptText()).thenReturn("药品目录");
        PrescriptionItemRequest item = new PrescriptionItemRequest();
        item.setDrugName("布洛�?);
        item.setCatalogMatched(true);
        when(drugCatalogService.recommendFromContext(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(item));
        when(drugCatalogService.retainCatalogMatched(any())).thenReturn(List.of(item));

        var result = aiPrescriptionService.suggestPrescription(1L, "头痛", "发热", "感冒");

        assertFalse(result.isAiAvailable());
        assertEquals(1, result.getRecommendedItems().size());
    }

    @Test
    @DisplayName("正常：青霉素过敏审核�?HIGH 风险")
    void reviewPrescription_penicillinAllergyHigh() {
        Patient patient = new Patient();
        patient.setAllergyHistory("青霉素过�?);
        when(patientMapper.selectById(1L)).thenReturn(patient);

        PrescriptionItemRequest item = new PrescriptionItemRequest();
        item.setDrugName("阿莫西林");

        var review = aiPrescriptionService.reviewPrescription(1L, "感染", List.of(item));

        assertEquals(RiskLevel.HIGH, review.getRiskLevel());
        assertFalse(review.getInteractions().isEmpty());
    }

    @Test
    @DisplayName("正常：阿司匹�?硝酸甘油�?MEDIUM 风险")
    void reviewPrescription_aspirinNitroMedium() {
        Patient patient = new Patient();
        when(patientMapper.selectById(1L)).thenReturn(patient);

        PrescriptionItemRequest aspirin = new PrescriptionItemRequest();
        aspirin.setDrugName("阿司匹林");
        PrescriptionItemRequest nitro = new PrescriptionItemRequest();
        nitro.setDrugName("硝酸甘油");

        var review = aiPrescriptionService.reviewPrescription(1L, "冠心�?, List.of(aspirin, nitro));

        assertEquals(RiskLevel.MEDIUM, review.getRiskLevel());
    }

    @Test
    @DisplayName("正常：库存检�?)
    void reviewPrescription_inventoryCheck() {
        Patient patient = new Patient();
        when(patientMapper.selectById(1L)).thenReturn(patient);
        Drug drug = new Drug();
        drug.setStockQuantity(0);
        when(drugCatalogService.getActiveDrug(1L)).thenReturn(drug);

        PrescriptionItemRequest item = new PrescriptionItemRequest();
        item.setDrugId(1L);
        item.setDrugName("测试�?);

        var review = aiPrescriptionService.reviewPrescription(1L, "测试", List.of(item));

        assertTrue(review.getInventoryAdvice().contains("库存不足"));
    }
}
