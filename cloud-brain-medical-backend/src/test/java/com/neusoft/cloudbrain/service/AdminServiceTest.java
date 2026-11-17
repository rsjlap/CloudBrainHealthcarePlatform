package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.UserRole;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.PasswordUtil;
import com.neusoft.cloudbrain.dto.request.admin.UpdateAdminStaffRequest;
import com.neusoft.cloudbrain.dto.response.admin.AdminDashboardVO;
import com.neusoft.cloudbrain.dto.response.admin.AdminUserVO;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService 管理后台")
class AdminServiceTest {

    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DepartmentMapper departmentMapper;

    private PasswordUtil passwordUtil;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        passwordUtil = new PasswordUtil();
        adminService = new AdminService(registrationMapper, userMapper, passwordUtil, departmentMapper);
    }

    @Test
    @DisplayName("正常：仪表盘统计数据")
    void getDashboardStats() {
        when(registrationMapper.selectCount(any())).thenReturn(5L, 100L, 2L, 3L);
        when(userMapper.selectCount(any())).thenReturn(50L, 10L);
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("内科");
        when(departmentMapper.selectList(any())).thenReturn(List.of(dept));

        AdminDashboardVO stats = adminService.getDashboardStats();

        assertEquals(5L, stats.getTodayRegistrations());
        assertEquals(100L, stats.getTotalRegistrations());
        assertEquals(7, stats.getRegistrationTrend().size());
    }

    @Test
    @DisplayName("正常：列出全部用�?)
    void listUsers() {
        User user = buildStaff(1L, UserRole.ADMIN, "admin");
        when(userMapper.selectList(any())).thenReturn(List.of(user));

        List<AdminUserVO> result = adminService.listUsers();

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
    }

    @Test
    @DisplayName("边界：查�?PATIENT 角色员工返回�?)
    void listStaff_patientRoleEmpty() {
        var result = adminService.listStaff(UserRole.PATIENT, null, 1L, 10L);

        assertTrue(result.getRecords().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    @DisplayName("正常：更新工作人员信�?)
    void updateStaff_success() {
        User user = buildStaff(2L, UserRole.DOCTOR, "doctor1");
        user.setPhone("13812345678");
        when(userMapper.selectById(2L)).thenReturn(user);

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest();
        request.setRealName("新名�?);
        request.setPassword("newpass123");

        AdminUserVO vo = adminService.updateStaff(2L, request);

        verify(userMapper).updateById(user);
        assertEquals("新名�?, vo.getRealName());
    }

    @Test
    @DisplayName("异常：工作人员不存在")
    void updateStaff_notFound() {
        when(userMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.updateStaff(99L, new UpdateAdminStaffRequest()));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
    }

    private static User buildStaff(Long id, UserRole role, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName("测试用户");
        user.setRole(role);
        user.setStatus(1);
        return user;
    }
}
