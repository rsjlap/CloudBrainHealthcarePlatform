package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.UserRole;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.JwtUtil;
import com.neusoft.cloudbrain.common.util.PasswordUtil;
import com.neusoft.cloudbrain.config.JwtConfig;
import com.neusoft.cloudbrain.dto.request.auth.LoginRequest;
import com.neusoft.cloudbrain.dto.response.auth.LoginResponse;
import com.neusoft.cloudbrain.dto.response.auth.UserInfoVO;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 登录与用户查询")
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    private PasswordUtil passwordUtil;
    private JwtUtil jwtUtil;
    private JwtConfig jwtConfig;
    private AuthService authService;

    private User activeDoctor;

    @BeforeEach
    void setUp() {
        passwordUtil = new PasswordUtil();
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("unit-test-jwt-secret-key-256bits-minimum!!");
        jwtConfig.setExpirationMs(86400000L);
        jwtUtil = new JwtUtil(jwtConfig);
        authService = new AuthService(userMapper, passwordUtil, jwtUtil, jwtConfig);

        activeDoctor = new User();
        activeDoctor.setId(2L);
        activeDoctor.setUsername("doctor1");
        activeDoctor.setPasswordHash(passwordUtil.encode("doctor123"));
        activeDoctor.setRole(UserRole.DOCTOR);
        activeDoctor.setRealName("张医生");
        activeDoctor.setPhone("13812345678");
        activeDoctor.setStatus(1);
    }

    @Test
    @DisplayName("正常：登录成功返回 Token")
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("doctor1");
        request.setPassword("doctor123");
        when(userMapper.selectOne(any())).thenReturn(activeDoctor);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(2L, response.getUserId());
        assertEquals(UserRole.DOCTOR, response.getRole());
        assertEquals("张医生", response.getRealName());
    }

    @Test
    @DisplayName("异常：用户不存在")
    void login_userNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("unknown");
        request.setPassword("x");
        when(userMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("异常：密码错误")
    void login_wrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("doctor1");
        request.setPassword("wrong");
        when(userMapper.selectOne(any())).thenReturn(activeDoctor);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("异常：账号被禁用")
    void login_disabledAccount() {
        activeDoctor.setStatus(0);
        LoginRequest request = new LoginRequest();
        request.setUsername("doctor1");
        request.setPassword("doctor123");
        when(userMapper.selectOne(any())).thenReturn(activeDoctor);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals(ResultCode.FORBIDDEN, ex.getResultCode());
        assertEquals("账号已被禁用", ex.getMessage());
    }

    @Test
    @DisplayName("正常：查询当前用户并脱敏手机号")
    void getCurrentUser_success() {
        when(userMapper.selectById(2L)).thenReturn(activeDoctor);

        UserInfoVO vo = authService.getCurrentUser(2L);

        assertEquals(2L, vo.getUserId());
        assertEquals("doctor1", vo.getUsername());
        assertEquals("138****5678", vo.getPhone());
        assertEquals(UserRole.DOCTOR, vo.getRole());
    }

    @Test
    @DisplayName("异常：用户不存在")
    void getCurrentUser_notFound() {
        when(userMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.getCurrentUser(99L));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
        assertEquals("用户不存在", ex.getMessage());
    }
}
