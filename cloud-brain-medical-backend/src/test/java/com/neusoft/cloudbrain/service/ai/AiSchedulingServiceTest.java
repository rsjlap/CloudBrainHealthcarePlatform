package com.neusoft.cloudbrain.service.ai;

import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.DoctorMapper;
import com.neusoft.cloudbrain.mapper.ScheduleMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiSchedulingService AI 排班生成")
class AiSchedulingServiceTest {

    @Mock
    private DoctorMapper doctorMapper;
    @Mock
    private ScheduleMapper scheduleMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DepartmentMapper departmentMapper;
    @Mock
    private AiPromptService aiPromptService;
    @Mock
    private AiLlmClient aiLlmClient;

    private AiSchedulingService aiSchedulingService;

    @BeforeEach
    void setUp() {
        aiSchedulingService = new AiSchedulingService(
                doctorMapper, scheduleMapper, userMapper, departmentMapper,
                aiPromptService, aiLlmClient);
    }

    @Test
    @DisplayName("异常：参数为�?)
    void generateSchedule_nullParams() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiSchedulingService.generateSchedule(null, LocalDate.now(), LocalDate.now()));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：结束日期早于开始日�?)
    void generateSchedule_invalidDateRange() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiSchedulingService.generateSchedule(1L,
                        LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1)));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：AI 未启�?)
    void generateSchedule_aiUnavailable() {
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("内科");
        when(departmentMapper.selectById(1L)).thenReturn(dept);
        when(doctorMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());
        when(scheduleMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());
        when(aiLlmClient.isAvailable()).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiSchedulingService.generateSchedule(1L,
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7)));
        assertEquals(ResultCode.SERVER_ERROR, ex.getResultCode());
    }
}
