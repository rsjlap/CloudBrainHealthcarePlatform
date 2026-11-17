package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.ExamOrderStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.request.exam.SaveExamReportRequest;
import com.neusoft.cloudbrain.dto.response.ai.ExamReportDraftVO;
import com.neusoft.cloudbrain.dto.response.exam.ExamReportVO;
import com.neusoft.cloudbrain.entity.ExamOrder;
import com.neusoft.cloudbrain.entity.ExamReport;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.ExamOrderMapper;
import com.neusoft.cloudbrain.mapper.ExamReportMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import com.neusoft.cloudbrain.service.ai.AiExamReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamReportService 检查报�?)
class ExamReportServiceTest {

    @Mock
    private ExamReportMapper examReportMapper;
    @Mock
    private ExamOrderMapper examOrderMapper;
    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AiExamReportService aiExamReportService;

    private ExamReportService examReportService;

    @BeforeEach
    void setUp() {
        examReportService = new ExamReportService(
                examReportMapper, examOrderMapper, registrationMapper, userMapper, aiExamReportService);
    }

    @Test
    @DisplayName("正常：AI 生成并保存报�?)
    void generateAndSave_success() {
        ExamOrder order = paidOrder(1L);
        order.setStatus(ExamOrderStatus.PENDING);
        order.setExamType("心电�?);
        when(examOrderMapper.selectById(1L)).thenReturn(order);
        when(userMapper.selectById(30L)).thenReturn(inspectorUser());
        when(aiExamReportService.generateExamReport(any(), any(), any()))
                .thenReturn(ExamReportDraftVO.builder().reportText("报告正文").conclusion("正常").build());
        when(examReportMapper.selectOne(any())).thenReturn(null);

        ExamReportVO vo = examReportService.generateAndSave(30L, 1L);

        verify(examReportMapper).insert(any(ExamReport.class));
        verify(examOrderMapper).updateById(order);
        assertEquals(ExamOrderStatus.IN_PROGRESS, order.getStatus());
        assertEquals("报告正文", vo.getReportText());
    }

    @Test
    @DisplayName("异常：已完成订单不可重复生成")
    void generateAndSave_alreadyDone() {
        ExamOrder order = paidOrder(1L);
        order.setStatus(ExamOrderStatus.DONE);
        when(examOrderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examReportService.generateAndSave(30L, 1L));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("正常：手动保存报�?)
    void save_success() {
        ExamOrder order = paidOrder(1L);
        when(examOrderMapper.selectById(1L)).thenReturn(order);
        when(userMapper.selectById(30L)).thenReturn(inspectorUser());
        when(examReportMapper.selectOne(any())).thenReturn(null);

        SaveExamReportRequest request = new SaveExamReportRequest();
        request.setOrderId(1L);
        request.setReportText("手动报告");
        request.setConclusion("异常");

        ExamReportVO vo = examReportService.save(30L, request);

        verify(examReportMapper).insert(any(ExamReport.class));
        assertEquals("手动报告", vo.getReportText());
    }

    @Test
    @DisplayName("正常：确认报告并完成订单")
    void confirm_success() {
        ExamReport report = new ExamReport();
        report.setId(5L);
        report.setOrderId(1L);
        report.setReportText("报告");
        report.setInspectorId(30L);
        ExamOrder order = paidOrder(1L);
        when(examReportMapper.selectById(5L)).thenReturn(report);
        when(examOrderMapper.selectById(1L)).thenReturn(order);
        when(userMapper.selectById(30L)).thenReturn(inspectorUser());

        ExamReportVO vo = examReportService.confirm(5L);

        verify(examOrderMapper).updateById(order);
        assertEquals(ExamOrderStatus.DONE, order.getStatus());
        assertNotNull(vo.getConfirmedAt());
    }

    @Test
    @DisplayName("异常：检查费未缴�?)
    void confirm_unpaidOrder() {
        ExamReport report = new ExamReport();
        report.setId(5L);
        report.setOrderId(1L);
        ExamOrder order = paidOrder(1L);
        order.setPaidAt(null);
        when(examReportMapper.selectById(5L)).thenReturn(report);
        when(examOrderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examReportService.confirm(5L));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    private static ExamOrder paidOrder(Long id) {
        ExamOrder order = new ExamOrder();
        order.setId(id);
        order.setRegistrationId(100L);
        order.setOrderedBy(20L);
        order.setPaidAt(LocalDateTime.now());
        order.setExamType("血常规");
        return order;
    }

    private static User inspectorUser() {
        User user = new User();
        user.setId(30L);
        user.setRealName("检验师");
        return user;
    }
}
