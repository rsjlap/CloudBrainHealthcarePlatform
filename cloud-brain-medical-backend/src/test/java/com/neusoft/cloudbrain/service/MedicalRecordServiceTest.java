package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.MedicalRecordStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.request.medical.SaveMedicalRecordRequest;
import com.neusoft.cloudbrain.dto.response.medical.MedicalRecordVO;
import com.neusoft.cloudbrain.entity.MedicalRecord;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.MedicalRecordMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicalRecordService 病历管理")
class MedicalRecordServiceTest {

    @Mock
    private MedicalRecordMapper medicalRecordMapper;
    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private UserMapper userMapper;

    private MedicalRecordService medicalRecordService;

    @BeforeEach
    void setUp() {
        medicalRecordService = new MedicalRecordService(medicalRecordMapper, registrationMapper, userMapper);
    }

    @Test
    @DisplayName("正常：新建病�?)
    void save_insertNewRecord() {
        Registration reg = paidRegistration(1L, 10L, 20L);
        when(registrationMapper.selectById(1L)).thenReturn(reg);
        when(medicalRecordMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectById(10L)).thenReturn(patientUser());
        when(userMapper.selectById(20L)).thenReturn(doctorUser());

        SaveMedicalRecordRequest request = new SaveMedicalRecordRequest();
        request.setRegistrationId(1L);
        request.setChiefComplaint("头痛");
        request.setDiagnosis("偏头�?);

        MedicalRecordVO vo = medicalRecordService.save(20L, request);

        verify(medicalRecordMapper).insert(any(MedicalRecord.class));
        assertEquals("头痛", vo.getChiefComplaint());
    }

    @Test
    @DisplayName("异常：挂号未缴费")
    void save_unpaidRegistration() {
        Registration reg = paidRegistration(1L, 10L, 20L);
        reg.setPaidAt(null);
        when(registrationMapper.selectById(1L)).thenReturn(reg);

        SaveMedicalRecordRequest request = new SaveMedicalRecordRequest();
        request.setRegistrationId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.save(20L, request));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：非接诊医生无权操作")
    void save_forbiddenDoctor() {
        Registration reg = paidRegistration(1L, 10L, 99L);
        when(registrationMapper.selectById(1L)).thenReturn(reg);

        SaveMedicalRecordRequest request = new SaveMedicalRecordRequest();
        request.setRegistrationId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.save(20L, request));
        assertEquals(ResultCode.FORBIDDEN, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：无效病历状�?)
    void save_invalidStatus() {
        Registration reg = paidRegistration(1L, 10L, 20L);
        when(registrationMapper.selectById(1L)).thenReturn(reg);
        when(medicalRecordMapper.selectOne(any())).thenReturn(null);

        SaveMedicalRecordRequest request = new SaveMedicalRecordRequest();
        request.setRegistrationId(1L);
        request.setStatus("INVALID");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> medicalRecordService.save(20L, request));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("正常：确认病�?)
    void confirm_success() {
        MedicalRecord record = new MedicalRecord();
        record.setId(5L);
        record.setRegistrationId(1L);
        record.setPatientId(10L);
        record.setDoctorId(20L);
        record.setStatus(MedicalRecordStatus.DRAFT);
        Registration reg = paidRegistration(1L, 10L, 20L);
        when(medicalRecordMapper.selectById(5L)).thenReturn(record);
        when(registrationMapper.selectById(1L)).thenReturn(reg);
        when(userMapper.selectById(10L)).thenReturn(patientUser());
        when(userMapper.selectById(20L)).thenReturn(doctorUser());

        MedicalRecordVO vo = medicalRecordService.confirm(5L, 20L);

        verify(medicalRecordMapper).updateById(record);
        assertEquals(MedicalRecordStatus.CONFIRMED, vo.getStatus());
        assertNotNull(vo.getConfirmedAt());
    }

    @Test
    @DisplayName("正常：按挂号查询病历，无记录返回 null")
    void getByRegistration_nullWhenMissing() {
        Registration reg = paidRegistration(1L, 10L, 20L);
        when(registrationMapper.selectById(1L)).thenReturn(reg);
        when(medicalRecordMapper.selectOne(any())).thenReturn(null);

        assertNull(medicalRecordService.getByRegistration(1L, 20L));
    }

    @Test
    @DisplayName("正常：医生历史按姓名与日期过�?)
    void listByDoctor_filtersByNameAndDate() {
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setRegistrationId(1L);
        record.setPatientId(10L);
        record.setDoctorId(20L);
        record.setStatus(MedicalRecordStatus.CONFIRMED);
        record.setConfirmedAt(LocalDateTime.of(2026, 6, 15, 10, 0));
        when(medicalRecordMapper.selectList(any())).thenReturn(List.of(record));
        User patient = patientUser();
        patient.setRealName("张三");
        when(userMapper.selectById(10L)).thenReturn(patient);
        when(userMapper.selectById(20L)).thenReturn(doctorUser());

        List<MedicalRecordVO> matched = medicalRecordService.listByDoctor(20L, "�?, LocalDate.of(2026, 6, 15));
        List<MedicalRecordVO> unmatched = medicalRecordService.listByDoctor(20L, "李四", null);

        assertEquals(1, matched.size());
        assertEquals(0, unmatched.size());
    }

    private static Registration paidRegistration(Long id, Long patientId, Long doctorId) {
        Registration reg = new Registration();
        reg.setId(id);
        reg.setPatientId(patientId);
        reg.setDoctorId(doctorId);
        reg.setPaidAt(LocalDateTime.now());
        return reg;
    }

    private static User patientUser() {
        User user = new User();
        user.setId(10L);
        user.setRealName("患者甲");
        return user;
    }

    private static User doctorUser() {
        User user = new User();
        user.setId(20L);
        user.setRealName("医生�?);
        return user;
    }
}
