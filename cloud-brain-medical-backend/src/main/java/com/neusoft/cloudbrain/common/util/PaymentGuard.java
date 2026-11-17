package com.neusoft.cloudbrain.common.util;

import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.entity.ExamOrder;
import com.neusoft.cloudbrain.entity.Prescription;
import com.neusoft.cloudbrain.entity.Registration;

public final class PaymentGuard {

    private PaymentGuard() {
    }

    public static void requireRegistrationPaid(Registration registration) {
        if (registration.getPaidAt() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "患者尚未缴纳挂号费，请先完成缴费");
        }
    }

    public static void requireExamOrderPaid(ExamOrder order) {
        if (order.getPaidAt() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "患者尚未缴纳检查费，请先完成缴费");
        }
    }

    public static void requirePrescriptionPaid(Prescription prescription) {
        if (prescription.getPaidAt() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "患者尚未缴纳处方费，请先完成缴费");
        }
    }
}
