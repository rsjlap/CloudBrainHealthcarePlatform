package com.neusoft.cloudbrain.service.ai;

import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.DrugMapper;
import com.neusoft.cloudbrain.mapper.MedicalEquipmentMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.service.AdminEquipmentService;
import com.neusoft.cloudbrain.service.AdminPatientService;
import com.neusoft.cloudbrain.service.AdminRegistrationService;
import com.neusoft.cloudbrain.service.AdminScheduleService;
import com.neusoft.cloudbrain.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAiQueryService 管理�?AI 问答")
class AdminAiQueryServiceTest {

    @Mock
    private AiLlmClient aiLlmClient;
    @Mock
    private AiPromptService aiPromptService;
    @Mock
    private AdminService adminService;
    @Mock
    private AdminRegistrationService adminRegistrationService;
    @Mock
    private AdminEquipmentService adminEquipmentService;
    @Mock
    private AdminPatientService adminPatientService;
    @Mock
    private AdminScheduleService adminScheduleService;
    @Mock
    private DrugMapper drugMapper;
    @Mock
    private DepartmentMapper departmentMapper;
    @Mock
    private MedicalEquipmentMapper equipmentMapper;
    @Mock
    private RegistrationMapper registrationMapper;

    private AdminAiQueryService adminAiQueryService;

    @BeforeEach
    void setUp() {
        adminAiQueryService = new AdminAiQueryService(
                aiLlmClient, aiPromptService, adminService, adminRegistrationService,
                adminEquipmentService, adminPatientService, adminScheduleService,
                drugMapper, departmentMapper, equipmentMapper, registrationMapper);
    }

    @Test
    @DisplayName("异常：问题为�?)
    void chat_emptyQuestion() {
        BusinessException ex = assertThrows(BusinessException.class, () -> adminAiQueryService.chat("  "));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：AI 未启�?)
    void chat_aiUnavailable() {
        when(aiLlmClient.isAvailable()).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminAiQueryService.chat("今天挂号多少人？"));
        assertEquals(ResultCode.SERVER_ERROR, ex.getResultCode());
    }
}
