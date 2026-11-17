package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.request.registration.CreateRegistrationRequest;
import com.neusoft.cloudbrain.dto.response.registration.RegistrationVO;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.Doctor;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.entity.Schedule;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.DoctorMapper;
import com.neusoft.cloudbrain.mapper.PatientMapper;
import com.neusoft.cloudbrain.mapper.PrescriptionMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.ScheduleMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService 挂号与叫号")
class RegistrationServiceTest {

    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DoctorMapper doctorMapper;
    @Mock
    private ScheduleMapper scheduleMapper;
    @Mock
    private PatientMapper patientMapper;
    @Mock
    private PrescriptionMapper prescriptionMapper;
    @Mock
    private DepartmentMapper departmentMapper;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                registrationMapper,
                userMapper,
                doctorMapper,
                scheduleMapper,
                patientMapper,
                prescriptionMapper,
                departmentMapper
        );
    }

    @Test
    @DisplayName("正常：患者挂号成功")
    void create_success() {
        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setDoctorId(2L);
        request.setScheduleId(1L);
        request.setChiefComplaint("头痛");

        User patient = new User();
        patient.setId(3L);
        patient.setRealName("患者甲");
        Doctor doctor = new Doctor();
        doctor.setId(2L);
        doctor.setDepartmentId(1L);
        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setRegisteredCount(5);
        schedule.setMaxPatients(20);
        Department department = new Department();
        department.setName("内科");

        when(userMapper.selectById(3L)).thenReturn(patient);
        when(doctorMapper.selectById(2L)).thenReturn(doctor);
        when(scheduleMapper.selectById(1L)).thenReturn(schedule);
        when(registrationMapper.selectCount(null)).thenReturn(10L);
        when(userMapper.selectById(eq(3L))).thenReturn(patient);
        when(userMapper.selectById(eq(2L))).thenReturn(new User());
        when(departmentMapper.selectById(1L)).thenReturn(department);
        when(scheduleMapper.selectById(1L)).thenReturn(schedule);

        RegistrationVO vo = registrationService.create(3L, request);

        assertEquals(RegistrationStatus.PENDING, vo.getStatus());
        assertEquals(6, vo.getQueueNo());
        assertFalse(vo.getPaid());
        verify(registrationMapper).insert(any(Registration.class));
        verify(scheduleMapper).updateById(schedule);
        assertEquals(6, schedule.getRegisteredCount());
    }

    @Test
    @DisplayName("异常：患者不存在")
    void create_patientNotFound() {
        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setDoctorId(2L);
        request.setScheduleId(1L);
        when(userMapper.selectById(3L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> registrationService.create(3L, request));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
        assertEquals("患者不存在", ex.getMessage());
    }

    @Test
    @DisplayName("边界：号源已满")
    void create_scheduleFull() {
        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setDoctorId(2L);
        request.setScheduleId(1L);

        when(userMapper.selectById(3L)).thenReturn(new User());
        when(doctorMapper.selectById(2L)).thenReturn(new Doctor());
        Schedule schedule = new Schedule();
        schedule.setRegisteredCount(20);
        schedule.setMaxPatients(20);
        when(scheduleMapper.selectById(1L)).thenReturn(schedule);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> registrationService.create(3L, request));
        assertEquals("号源已满", ex.getMessage());
    }

    @Test
    @DisplayName("正常：叫号成功（已缴费）")
    void callNext_success() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(2L);
        registration.setPatientId(3L);
        registration.setScheduleId(1L);
        registration.setDepartmentId(1L);
        registration.setPaidAt(LocalDateTime.now());
        registration.setStatus(RegistrationStatus.PENDING);

        when(registrationMapper.selectById(10L)).thenReturn(registration);
        when(userMapper.selectById(any())).thenReturn(new User());
        when(departmentMapper.selectById(1L)).thenReturn(new Department());
        when(scheduleMapper.selectById(1L)).thenReturn(new Schedule());

        RegistrationVO vo = registrationService.callNext(10L, 2L);

        assertEquals(RegistrationStatus.IN_PROGRESS, vo.getStatus());
        verify(registrationMapper).updateById(registration);
    }

    @Test
    @DisplayName("异常：未缴费不能叫号")
    void callNext_unpaidRegistration() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(2L);
        registration.setPaidAt(null);
        when(registrationMapper.selectById(10L)).thenReturn(registration);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> registrationService.callNext(10L, 2L));
        assertEquals("患者尚未缴纳挂号费，请先完成缴费", ex.getMessage());
    }

    @Test
    @DisplayName("异常：非本医生无权叫号")
    void callNext_forbidden() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setDoctorId(99L);
        registration.setPaidAt(LocalDateTime.now());
        when(registrationMapper.selectById(10L)).thenReturn(registration);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> registrationService.callNext(10L, 2L));
        assertEquals(ResultCode.FORBIDDEN, ex.getResultCode());
    }

    @Test
    @DisplayName("正常：按患者查询挂号列表")
    void listByPatient_success() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setPatientId(3L);
        registration.setDoctorId(2L);
        registration.setScheduleId(1L);
        registration.setDepartmentId(1L);
        registration.setStatus(RegistrationStatus.PENDING);

        when(registrationMapper.selectList(any())).thenReturn(List.of(registration));
        when(userMapper.selectById(any())).thenReturn(new User());
        when(departmentMapper.selectById(any())).thenReturn(new Department());
        Schedule schedule = new Schedule();
        schedule.setWorkDate(LocalDate.now());
        when(scheduleMapper.selectById(1L)).thenReturn(schedule);

        List<RegistrationVO> list = registrationService.listByPatient(3L);

        assertEquals(1, list.size());
        assertEquals(10L, list.get(0).getId());
    }

    @Test
    @DisplayName("边界：患者无挂号记录")
    void listByPatient_empty() {
        when(registrationMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertTrue(registrationService.listByPatient(3L).isEmpty());
    }
}
