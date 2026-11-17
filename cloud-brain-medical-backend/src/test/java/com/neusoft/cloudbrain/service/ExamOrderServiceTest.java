package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.ExamOrderStatus;
import com.neusoft.cloudbrain.common.enums.ExamOrderType;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.request.exam.CreateExamOrderRequest;
import com.neusoft.cloudbrain.dto.response.exam.ExamOrderVO;
import com.neusoft.cloudbrain.entity.ExamItem;
import com.neusoft.cloudbrain.entity.ExamOrder;
import com.neusoft.cloudbrain.entity.LabItem;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.ExamOrderMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamOrderService 检查申�?)
class ExamOrderServiceTest {

    @Mock
    private ExamOrderMapper examOrderMapper;
    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ExamCatalogService examCatalogService;

    private ExamOrderService examOrderService;

    @BeforeEach
    void setUp() {
        examOrderService = new ExamOrderService(examOrderMapper, registrationMapper, userMapper, examCatalogService);
    }

    @Test
    @DisplayName("正常：创建检验申�?)
    void create_labOrder() {
        Registration reg = paidRegistration();
        when(registrationMapper.selectById(1L)).thenReturn(reg);
        when(userMapper.selectById(20L)).thenReturn(doctorUser());
        LabItem labItem = new LabItem();
        labItem.setId(100L);
        labItem.setItemName("血常规");
        labItem.setCategory("血�?);
        labItem.setFee(new BigDecimal("50"));
        when(examCatalogService.getEnabledLabItem(100L)).thenReturn(labItem);
        when(registrationMapper.selectById(1L)).thenReturn(reg);
        when(userMapper.selectById(10L)).thenReturn(patientUser());
        when(examCatalogService.resolveOrderFee(any())).thenReturn(new BigDecimal("50"));

        CreateExamOrderRequest request = new CreateExamOrderRequest();
        request.setRegistrationId(1L);
        request.setOrderType(ExamOrderType.LAB);
        request.setLabItemId(100L);
        request.setClinicalIndication("发热待查");

        ExamOrderVO vo = examOrderService.create(20L, request);

        verify(examOrderMapper).insert(any(ExamOrder.class));
        assertEquals("血常规", vo.getExamType());
        assertEquals(ExamOrderType.LAB, vo.getOrderType());
    }

    @Test
    @DisplayName("正常：创建自由文本检查申�?)
    void create_freeTextExam() {
        Registration reg = paidRegistration();
        when(registrationMapper.selectById(1L)).thenReturn(reg);
        when(userMapper.selectById(20L)).thenReturn(doctorUser());
        when(registrationMapper.selectById(1L)).thenReturn(reg);
        when(userMapper.selectById(10L)).thenReturn(patientUser());
        when(examCatalogService.resolveOrderFee(any())).thenReturn(new BigDecimal("80"));

        CreateExamOrderRequest request = new CreateExamOrderRequest();
        request.setRegistrationId(1L);
        request.setExamType("胸部CT");
        request.setBodyPart("胸部");

        ExamOrderVO vo = examOrderService.create(20L, request);

        assertEquals("胸部CT", vo.getExamType());
    }

    @Test
    @DisplayName("异常：未选择检查项�?)
    void create_missingItems() {
        Registration reg = paidRegistration();
        when(registrationMapper.selectById(1L)).thenReturn(reg);
        when(userMapper.selectById(20L)).thenReturn(doctorUser());

        CreateExamOrderRequest request = new CreateExamOrderRequest();
        request.setRegistrationId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examOrderService.create(20L, request));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：检验项目不存在或已停用")
    void create_disabledLabItem() {
        Registration reg = paidRegistration();
        when(registrationMapper.selectById(1L)).thenReturn(reg);
        when(userMapper.selectById(20L)).thenReturn(doctorUser());
        when(examCatalogService.getEnabledLabItem(999L)).thenReturn(null);

        CreateExamOrderRequest request = new CreateExamOrderRequest();
        request.setRegistrationId(1L);
        request.setLabItemId(999L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examOrderService.create(20L, request));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
    }

    @Test
    @DisplayName("正常：查询待处理申请")
    void listPendingForInspector() {
        ExamOrder order = new ExamOrder();
        order.setId(1L);
        order.setRegistrationId(1L);
        order.setOrderedBy(20L);
        order.setStatus(ExamOrderStatus.PENDING);
        order.setPaidAt(LocalDateTime.now());
        order.setExamType("心电�?);
        when(examOrderMapper.selectList(any())).thenReturn(List.of(order));
        when(registrationMapper.selectById(1L)).thenReturn(paidRegistration());
        when(userMapper.selectById(10L)).thenReturn(patientUser());
        when(userMapper.selectById(20L)).thenReturn(doctorUser());
        when(examCatalogService.resolveOrderFee(any())).thenReturn(new BigDecimal("30"));

        List<ExamOrderVO> result = examOrderService.listPendingForInspector();

        assertEquals(1, result.size());
        assertEquals("心电�?, result.get(0).getExamType());
    }

    private static Registration paidRegistration() {
        Registration reg = new Registration();
        reg.setId(1L);
        reg.setPatientId(10L);
        reg.setDoctorId(20L);
        reg.setPaidAt(LocalDateTime.now());
        return reg;
    }

    private static User doctorUser() {
        User user = new User();
        user.setId(20L);
        user.setRealName("检查医�?);
        return user;
    }

    private static User patientUser() {
        User user = new User();
        user.setId(10L);
        user.setRealName("患�?);
        return user;
    }
}
