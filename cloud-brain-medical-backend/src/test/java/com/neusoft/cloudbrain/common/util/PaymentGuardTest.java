package com.neusoft.cloudbrain.common.util;

import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.entity.ExamOrder;
import com.neusoft.cloudbrain.entity.Prescription;
import com.neusoft.cloudbrain.entity.Registration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PaymentGuard 缴费校验")
class PaymentGuardTest {

    @Test
    @DisplayName("正常：挂号已缴费不抛异常")
    void requireRegistrationPaid_whenPaid() {
        Registration registration = new Registration();
        registration.setPaidAt(LocalDateTime.now());
        assertDoesNotThrow(() -> PaymentGuard.requireRegistrationPaid(registration));
    }

    @Test
    @DisplayName("异常：挂号未缴费")
    void requireRegistrationPaid_whenUnpaid() {
        Registration registration = new Registration();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> PaymentGuard.requireRegistrationPaid(registration));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
        assertEquals("患者尚未缴纳挂号费，请先完成缴费", ex.getMessage());
    }

    @Test
    @DisplayName("正常：检查单已缴费")
    void requireExamOrderPaid_whenPaid() {
        ExamOrder order = new ExamOrder();
        order.setPaidAt(LocalDateTime.now());
        assertDoesNotThrow(() -> PaymentGuard.requireExamOrderPaid(order));
    }

    @Test
    @DisplayName("异常：检查单未缴费")
    void requireExamOrderPaid_whenUnpaid() {
        ExamOrder order = new ExamOrder();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> PaymentGuard.requireExamOrderPaid(order));
        assertEquals("患者尚未缴纳检查费，请先完成缴费", ex.getMessage());
    }

    @Test
    @DisplayName("正常：处方已缴费")
    void requirePrescriptionPaid_whenPaid() {
        Prescription prescription = new Prescription();
        prescription.setPaidAt(LocalDateTime.now());
        assertDoesNotThrow(() -> PaymentGuard.requirePrescriptionPaid(prescription));
    }

    @Test
    @DisplayName("异常：处方未缴费")
    void requirePrescriptionPaid_whenUnpaid() {
        Prescription prescription = new Prescription();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> PaymentGuard.requirePrescriptionPaid(prescription));
        assertEquals("患者尚未缴纳处方费，请先完成缴费", ex.getMessage());
    }
}
