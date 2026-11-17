package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.response.registration.RegistrationVO;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminRegistrationService 挂号管理")
class AdminRegistrationServiceTest {

    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private RegistrationService registrationService;

    private AdminRegistrationService adminRegistrationService;

    @BeforeEach
    void setUp() {
        adminRegistrationService = new AdminRegistrationService(registrationMapper, registrationService);
    }

    @Test
    @DisplayName("正常：分页搜索挂�?)
    void search_returnsPage() {
        Registration reg = new Registration();
        reg.setId(1L);
        Page<Registration> page = new Page<>(1, 10);
        page.setRecords(List.of(reg));
        page.setTotal(1);
        when(registrationMapper.adminSearchPage(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);
        RegistrationVO vo = RegistrationVO.builder().id(1L).build();
        when(registrationService.toAdminVO(reg)).thenReturn(vo);

        PageResult<RegistrationVO> result = adminRegistrationService.search(
                "�?, RegistrationStatus.PENDING, 1L, 2L, true,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 1L, 10L);

        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("正常：获取挂号详�?)
    void getDetail_success() {
        Registration reg = new Registration();
        reg.setId(5L);
        when(registrationMapper.selectById(5L)).thenReturn(reg);
        RegistrationVO vo = RegistrationVO.builder().id(5L).build();
        when(registrationService.toAdminVO(reg)).thenReturn(vo);

        RegistrationVO result = adminRegistrationService.getDetail(5L);

        assertEquals(5L, result.getId());
    }

    @Test
    @DisplayName("异常：挂号不存在")
    void getDetail_notFound() {
        when(registrationMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminRegistrationService.getDetail(99L));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
    }
}
