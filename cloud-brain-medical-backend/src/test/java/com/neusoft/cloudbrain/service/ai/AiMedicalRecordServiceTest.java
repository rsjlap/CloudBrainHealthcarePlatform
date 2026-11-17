package com.neusoft.cloudbrain.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.entity.Patient;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.mapper.ExamOrderMapper;
import com.neusoft.cloudbrain.mapper.ExamReportMapper;
import com.neusoft.cloudbrain.mapper.PatientMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import com.neusoft.cloudbrain.service.ExamCatalogService;
import com.neusoft.cloudbrain.service.kg.KgDiseaseResolver;
import com.neusoft.cloudbrain.service.kg.KnowledgeGraphClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiMedicalRecordService AI 病历生成")
class AiMedicalRecordServiceTest {

    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private PatientMapper patientMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ExamOrderMapper examOrderMapper;
    @Mock
    private ExamReportMapper examReportMapper;
    @Mock
    private AiPromptService aiPromptService;
    @Mock
    private AiLlmClient aiLlmClient;
    @Mock
    private KnowledgeGraphClient knowledgeGraphClient;
    @Mock
    private ExamCatalogService examCatalogService;

    private AiMedicalRecordService aiMedicalRecordService;

    @BeforeEach
    void setUp() {
        aiMedicalRecordService = new AiMedicalRecordService(
                registrationMapper, patientMapper, userMapper, examOrderMapper, examReportMapper,
                new ObjectMapper(), aiPromptService, aiLlmClient, knowledgeGraphClient,
                new KgDiseaseResolver(), examCatalogService, new MedicalRecordSseEmitter());
        when(aiLlmClient.isAvailable()).thenReturn(false);
        when(examCatalogService.buildCatalogPromptText()).thenReturn("检查目�?);
        when(examCatalogService.recommendFromContext(any(), any(Integer.class))).thenReturn(Collections.emptyList());
        when(examCatalogService.retainCatalogMatched(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("异常：对话为�?)
    void generateMedicalRecord_emptyDialog() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiMedicalRecordService.generateMedicalRecord(1L, "  "));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("正常：规则引擎生成病�?)
    void generateMedicalRecord_rulesFallback() {
        Registration reg = paidRegistration();
        Patient patient = new Patient();
        patient.setAllergyHistory("�?);
        when(registrationMapper.selectById(1L)).thenReturn(reg);
        when(patientMapper.selectById(10L)).thenReturn(patient);
        when(examOrderMapper.selectList(any())).thenReturn(Collections.emptyList());

        var draft = aiMedicalRecordService.generateMedicalRecord(1L, "患者：医生您好，我头痛三天了�?);

        assertNotNull(draft.getChiefComplaint());
        assertEquals(false, draft.isAiAvailable());
    }

    @Test
    @DisplayName("边界：流式生成空对话发送错�?)
    void generateMedicalRecordStream_emptyDialog() {
        SseEmitter emitter = new SseEmitter();

        aiMedicalRecordService.generateMedicalRecordStream(1L, "", emitter);
    }

    private static Registration paidRegistration() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(10L);
        reg.setDoctorId(20L);
        reg.setPaidAt(LocalDateTime.now());
        return reg;
    }
}
