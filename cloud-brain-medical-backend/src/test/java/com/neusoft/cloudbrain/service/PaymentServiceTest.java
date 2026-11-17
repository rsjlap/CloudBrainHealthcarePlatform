package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.BillType;
import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.response.payment.PatientBillVO;
import com.neusoft.cloudbrain.entity.Doctor;
import com.neusoft.cloudbrain.entity.ExamOrder;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.DoctorMapper;
import com.neusoft.cloudbrain.mapper.ExamOrderMapper;
import com.neusoft.cloudbrain.mapper.PrescriptionItemMapper;
import com.neusoft.cloudbrain.mapper.PrescriptionMapper;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 缴费与账单")
class PaymentServiceTest {

    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private ExamOrderMapper examOrderMapper;
    @Mock
    private PrescriptionMapper prescriptionMapper;
    @Mock
    private PrescriptionItemMapper prescriptionItemMapper;
    @Mock
    private DoctorMapper doctorMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ExamCatalogService examCatalogService;
    @Mock
    private PrescriptionService prescriptionService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                registrationMapper,
                examOrderMapper,
                prescriptionMapper,
                prescriptionItemMapper,
                doctorMapper,
                userMapper,
                examCatalogService,
                prescriptionService
        );
    }

    @Test
    @DisplayName("正常：查询账单包含挂号费")
    void listMyBills_withRegistration() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setPatientId(3L);
        registration.setDoctorId(2L);
        registration.setCreatedAt(LocalDateTime.now());
        registration.setStatus(RegistrationStatus.PENDING);

        Doctor doctor = new Doctor();
        doctor.setId(2L);
        doctor.setRegistrationFee(new BigDecimal("15.00"));

        User doctorUser = new User();
        doctorUser.setRealName("张医生");

        when(registrationMapper.selectList(any())).thenReturn(List.of(registration));
        when(examOrderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(prescriptionMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(doctorMapper.selectById(2L)).thenReturn(doctor);
        when(userMapper.selectById(2L)).thenReturn(doctorUser);

        List<PatientBillVO> bills = paymentService.listMyBills(3L);

        assertEquals(1, bills.size());
        assertEquals(BillType.REGISTRATION, bills.get(0).getBillType());
        assertEquals(new BigDecimal("15.00"), bills.get(0).getAmount());
        assertFalse(bills.get(0).isPaid());
        assertTrue(bills.get(0).getTitle().contains("张医生"));
    }

    @Test
    @DisplayName("边界：无挂号记录返回空列表")
    void listMyBills_empty() {
        when(registrationMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertTrue(paymentService.listMyBills(3L).isEmpty());
    }

    @Test
    @DisplayName("正常：缴纳挂号费")
    void payRegistration_success() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setPatientId(3L);
        when(registrationMapper.selectById(10L)).thenReturn(registration);

        paymentService.payRegistration(3L, 10L);

        assertTrue(registration.getPaidAt() != null);
        verify(registrationMapper).updateById(registration);
    }

    @Test
    @DisplayName("异常：重复缴纳挂号费")
    void payRegistration_alreadyPaid() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setPatientId(3L);
        registration.setPaidAt(LocalDateTime.now());
        when(registrationMapper.selectById(10L)).thenReturn(registration);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.payRegistration(3L, 10L));
        assertEquals("挂号费已缴纳", ex.getMessage());
    }

    @Test
    @DisplayName("异常：非本人挂号无权缴费")
    void payRegistration_forbidden() {
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setPatientId(99L);
        when(registrationMapper.selectById(10L)).thenReturn(registration);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.payRegistration(3L, 10L));
        assertEquals(ResultCode.FORBIDDEN, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：检查单不存在")
    void payExamOrder_notFound() {
        when(examOrderMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.payExamOrder(3L, 1L));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
        assertEquals("检查单不存在", ex.getMessage());
    }

    @Test
    @DisplayName("正常：缴纳检查费")
    void payExamOrder_success() {
        ExamOrder order = new ExamOrder();
        order.setId(5L);
        order.setRegistrationId(10L);
        Registration registration = new Registration();
        registration.setId(10L);
        registration.setPatientId(3L);

        when(examOrderMapper.selectById(5L)).thenReturn(order);
        when(registrationMapper.selectById(10L)).thenReturn(registration);

        paymentService.payExamOrder(3L, 5L);

        assertTrue(order.getPaidAt() != null);
        verify(examOrderMapper).updateById(order);
    }

    @Test
    @DisplayName("异常：处方不存在")
    void payPrescription_notFound() {
        when(prescriptionMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.payPrescription(3L, 1L));
        assertEquals("处方不存在", ex.getMessage());
    }
}
