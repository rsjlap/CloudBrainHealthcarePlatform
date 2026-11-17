package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.cloudbrain.common.enums.UserRole;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.request.patient.UpdateAdminPatientRequest;
import com.neusoft.cloudbrain.entity.Patient;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.MedicalRecordMapper;
import com.neusoft.cloudbrain.mapper.PatientMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPatientService 患者管�?)
class AdminPatientServiceTest {

    @Mock
    private PatientMapper patientMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private MedicalRecordMapper medicalRecordMapper;
    @Mock
    private RegistrationService registrationService;
    @Mock
    private MedicalRecordService medicalRecordService;

    private AdminPatientService adminPatientService;

    @BeforeEach
    void setUp() {
        adminPatientService = new AdminPatientService(
                patientMapper, userMapper, registrationMapper, medicalRecordMapper,
                registrationService, medicalRecordService);
    }

    @Test
    @DisplayName("正常：患者统�?)
    void getStats() {
        when(userMapper.selectCount(any())).thenReturn(100L, 5L);
        when(patientMapper.selectCount(any())).thenReturn(50L, 50L);
        when(patientMapper.countWithAllergy()).thenReturn(10L);
        when(patientMapper.countWithRegistration()).thenReturn(80L);
        User patient = new User();
        patient.setStatus(1);
        patient.setRole(UserRole.PATIENT);
        when(userMapper.selectList(any())).thenReturn(List.of(patient));

        var stats = adminPatientService.getStats();

        assertEquals(100L, stats.getTotalCount());
        assertEquals(10L, stats.getAllergyCount());
    }

    @Test
    @DisplayName("正常：分页搜索患�?)
    void search() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setGender(1);
        Page<Patient> page = new Page<>(1, 10);
        page.setRecords(List.of(patient));
        page.setTotal(1);
        when(patientMapper.searchPage(any(), any(), any(), any(), any(), any())).thenReturn(page);
        User user = new User();
        user.setId(1L);
        user.setRealName("张三");
        user.setPhone("13812345678");
        when(userMapper.selectById(1L)).thenReturn(user);

        var result = adminPatientService.search(null, null, null, null, null, 1L, 10L);

        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("异常：患者不存在")
    void getDetail_notFound() {
        when(patientMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> adminPatientService.getDetail(99L));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
    }

    @Test
    @DisplayName("正常：更新患者信�?)
    void update_success() {
        Patient patient = new Patient();
        patient.setId(1L);
        User user = new User();
        user.setId(1L);
        user.setRealName("旧名");
        when(patientMapper.selectById(1L)).thenReturn(patient);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(registrationService.listByPatient(1L)).thenReturn(Collections.emptyList());
        when(medicalRecordService.listByPatient(1L)).thenReturn(Collections.emptyList());

        UpdateAdminPatientRequest request = new UpdateAdminPatientRequest();
        request.setRealName("新名");

        var detail = adminPatientService.update(1L, request);

        assertEquals("新名", detail.getRealName());
    }
}
