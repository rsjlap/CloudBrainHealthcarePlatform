package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.request.doctor.UpdateDoctorProfileRequest;
import com.neusoft.cloudbrain.dto.response.doctor.DoctorProfileVO;
import com.neusoft.cloudbrain.dto.response.registration.DoctorCardVO;
import com.neusoft.cloudbrain.dto.response.registration.ScheduleVO;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.Doctor;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.DoctorMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorService 医生信息")
class DoctorServiceTest {

    @Mock
    private DoctorMapper doctorMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DepartmentMapper departmentMapper;
    @Mock
    private ScheduleService scheduleService;

    private DoctorService doctorService;

    @BeforeEach
    void setUp() {
        doctorService = new DoctorService(doctorMapper, userMapper, departmentMapper, scheduleService);
    }

    @Test
    @DisplayName("正常：按科室列出医生")
    void listByDepartment_withoutDate() {
        Doctor doctor = buildDoctor(1L, 10L);
        when(doctorMapper.selectList(any())).thenReturn(List.of(doctor));
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "张医�?));
        when(departmentMapper.selectById(10L)).thenReturn(buildDepartment(10L, "内科"));

        List<DoctorCardVO> result = doctorService.listByDepartment(10L, null);

        assertEquals(1, result.size());
        assertEquals("张医�?, result.get(0).getRealName());
    }

    @Test
    @DisplayName("正常：按科室与日期列出可预约医生")
    void listByDepartment_withDate() {
        Doctor doctor = buildDoctor(1L, 10L);
        when(doctorMapper.findAvailableByDepartmentAndDate(eq(10L), eq(LocalDate.of(2026, 6, 15))))
                .thenReturn(List.of(doctor));
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "李医�?));
        when(departmentMapper.selectById(10L)).thenReturn(buildDepartment(10L, "外科"));

        List<DoctorCardVO> result = doctorService.listByDepartment(10L, LocalDate.of(2026, 6, 15));

        assertEquals("李医�?, result.get(0).getRealName());
    }

    @Test
    @DisplayName("异常：医生不存在")
    void getById_notFound() {
        when(doctorMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> doctorService.getById(99L));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
    }

    @Test
    @DisplayName("正常：获取医生档案并脱敏手机�?)
    void getProfile_masksPhone() {
        Doctor doctor = buildDoctor(2L, 10L);
        User user = buildUser(2L, "王医�?);
        user.setPhone("13812345678");
        when(doctorMapper.selectById(2L)).thenReturn(doctor);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(departmentMapper.selectById(10L)).thenReturn(buildDepartment(10L, "内科"));

        DoctorProfileVO profile = doctorService.getProfile(2L);

        assertEquals("王医�?, profile.getRealName());
        assertTrue(profile.getPhone().contains("*"));
    }

    @Test
    @DisplayName("正常：更新医生简�?)
    void updateProfile() {
        Doctor doctor = buildDoctor(2L, 10L);
        when(doctorMapper.selectById(2L)).thenReturn(doctor);
        when(userMapper.selectById(2L)).thenReturn(buildUser(2L, "王医�?));
        when(departmentMapper.selectById(10L)).thenReturn(buildDepartment(10L, "内科"));

        UpdateDoctorProfileRequest request = new UpdateDoctorProfileRequest();
        request.setSpecialty("心血�?);
        request.setIntroduction("擅长冠心�?);

        DoctorProfileVO profile = doctorService.updateProfile(2L, request);

        verify(doctorMapper).updateById(doctor);
        assertEquals("心血�?, profile.getSpecialty());
    }

    @Test
    @DisplayName("正常：查询我的排�?)
    void listMySchedules() {
        Doctor doctor = buildDoctor(2L, 10L);
        when(doctorMapper.selectById(2L)).thenReturn(doctor);
        ScheduleVO slot = ScheduleVO.builder().id(1L).doctorId(2L).build();
        when(scheduleService.listByDoctorInRange(2L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(List.of(slot));

        List<ScheduleVO> result = doctorService.listMySchedules(2L,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertEquals(1, result.size());
    }

    private static Doctor buildDoctor(Long id, Long deptId) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setDepartmentId(deptId);
        doctor.setTitle("主任医师");
        doctor.setRegistrationFee(new BigDecimal("20"));
        return doctor;
    }

    private static User buildUser(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setRealName(name);
        return user;
    }

    private static Department buildDepartment(Long id, String name) {
        Department dept = new Department();
        dept.setId(id);
        dept.setName(name);
        return dept;
    }
}
