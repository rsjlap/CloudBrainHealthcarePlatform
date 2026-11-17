package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.RiskLevel;
import com.neusoft.cloudbrain.common.enums.UserRole;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.PasswordUtil;
import com.neusoft.cloudbrain.dto.request.admin.SaveAdminDoctorRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDoctorService 医生管理")
class AdminDoctorServiceTest {

    @Mock
    private DoctorMapper doctorMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DepartmentMapper departmentMapper;

    private PasswordUtil passwordUtil;
    private AdminDoctorService adminDoctorService;

    @BeforeEach
    void setUp() {
        passwordUtil = new PasswordUtil();
        adminDoctorService = new AdminDoctorService(doctorMapper, userMapper, departmentMapper, passwordUtil);
    }

    @Test
    @DisplayName("异常：用户名已存�?)
    void create_duplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        SaveAdminDoctorRequest request = new SaveAdminDoctorRequest();
        request.setUsername("doctor1");
        request.setPassword("pass12345");
        request.setRealName("张医�?);
        request.setDepartmentId(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> adminDoctorService.create(request));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("正常：创建医�?)
    void create_success() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("内科");
        when(departmentMapper.selectById(1L)).thenReturn(dept);
        doAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setDepartmentId(1L);
        when(doctorMapper.selectById(10L)).thenReturn(doctor);
        User user = new User();
        user.setId(10L);
        user.setUsername("newdoc");
        user.setRealName("新医�?);
        user.setRole(UserRole.DOCTOR);
        user.setStatus(1);
        when(userMapper.selectById(10L)).thenReturn(user);

        SaveAdminDoctorRequest request = new SaveAdminDoctorRequest();
        request.setUsername("newdoc");
        request.setPassword("pass12345");
        request.setRealName("新医�?);
        request.setDepartmentId(1L);
        request.setRegistrationFee(new BigDecimal("15"));

        var vo = adminDoctorService.create(request);

        assertEquals("新医�?, vo.getRealName());
    }

    @Test
    @DisplayName("异常：医生不存在")
    void getDetail_notFound() {
        when(doctorMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> adminDoctorService.getDetail(99L));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
    }
}
