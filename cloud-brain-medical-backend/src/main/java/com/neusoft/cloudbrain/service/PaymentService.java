package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.enums.BillType;
import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.response.payment.PatientBillVO;
import com.neusoft.cloudbrain.entity.*;
import com.neusoft.cloudbrain.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RegistrationMapper registrationMapper;
    private final ExamOrderMapper examOrderMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final ExamCatalogService examCatalogService;
    private final PrescriptionService prescriptionService;

    @Transactional(readOnly = true)
    public List<PatientBillVO> listMyBills(Long patientId) {
        List<PatientBillVO> bills = new ArrayList<>();

        List<Registration> registrations = registrationMapper.selectList(
                Wrappers.<Registration>lambdaQuery()
                        .eq(Registration::getPatientId, patientId)
                        .ne(Registration::getStatus, RegistrationStatus.CANCELLED)
        );
        for (Registration registration : registrations) {
            bills.add(buildRegistrationBill(registration));
        }

        if (!registrations.isEmpty()) {
            List<Long> registrationIds = registrations.stream().map(Registration::getId).toList();
            List<ExamOrder> examOrders = examOrderMapper.selectList(
                    Wrappers.<ExamOrder>lambdaQuery()
                            .in(ExamOrder::getRegistrationId, registrationIds)
            );
            for (ExamOrder order : examOrders) {
                bills.add(buildExamBill(order));
            }

            List<Prescription> prescriptions = prescriptionMapper.selectList(
                    Wrappers.<Prescription>lambdaQuery()
                            .in(Prescription::getRegistrationId, registrationIds)
            );
            for (Prescription prescription : prescriptions) {
                bills.add(buildPrescriptionBill(prescription));
            }
        }

        bills.sort(Comparator.comparing(PatientBillVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return bills;
    }

    @Transactional
    public void payRegistration(Long patientId, Long registrationId) {
        Registration registration = getOwnedRegistration(registrationId, patientId);
        if (registration.getPaidAt() != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "挂号费已缴纳");
        }
        registration.setPaidAt(LocalDateTime.now());
        registrationMapper.updateById(registration);
    }

    @Transactional
    public void payExamOrder(Long patientId, Long examOrderId) {
        ExamOrder order = examOrderMapper.selectById(examOrderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查单不存在");
        }
        getOwnedRegistration(order.getRegistrationId(), patientId);
        if (order.getPaidAt() != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "检查费已缴纳");
        }
        order.setPaidAt(LocalDateTime.now());
        examOrderMapper.updateById(order);
    }

    @Transactional
    public void payPrescription(Long patientId, Long prescriptionId) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "处方不存在");
        }
        getOwnedRegistration(prescription.getRegistrationId(), patientId);
        if (prescription.getPaidAt() != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "处方费已缴纳");
        }
        prescription.setPaidAt(LocalDateTime.now());
        prescriptionMapper.updateById(prescription);
    }

    private PatientBillVO buildRegistrationBill(Registration registration) {
        Doctor doctor = doctorMapper.selectById(registration.getDoctorId());
        User doctorUser = userMapper.selectById(registration.getDoctorId());
        BigDecimal amount = doctor != null && doctor.getRegistrationFee() != null
                ? doctor.getRegistrationFee()
                : BigDecimal.ZERO;
        String doctorName = doctorUser != null ? doctorUser.getRealName() : "医生";
        return PatientBillVO.builder()
                .billType(BillType.REGISTRATION)
                .relatedId(registration.getId())
                .title("挂号费 - " + doctorName)
                .amount(amount)
                .paid(registration.getPaidAt() != null)
                .createdAt(registration.getCreatedAt())
                .build();
    }

    private PatientBillVO buildExamBill(ExamOrder order) {
        String examName = StringUtils.hasText(order.getExamType()) ? order.getExamType() : "检查/检验";
        String part = StringUtils.hasText(order.getBodyPart()) ? order.getBodyPart() : "";
        String title = part.isEmpty() ? examName : examName + "（" + part + "）";
        return PatientBillVO.builder()
                .billType(BillType.EXAM)
                .relatedId(order.getId())
                .title(title)
                .amount(examCatalogService.resolveOrderFee(order))
                .paid(order.getPaidAt() != null)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private PatientBillVO buildPrescriptionBill(Prescription prescription) {
        List<PrescriptionItem> items = prescriptionItemMapper.selectList(
                Wrappers.<PrescriptionItem>lambdaQuery()
                        .eq(PrescriptionItem::getPrescriptionId, prescription.getId())
        );
        return PatientBillVO.builder()
                .billType(BillType.PRESCRIPTION)
                .relatedId(prescription.getId())
                .title("处方费")
                .amount(prescriptionService.calculatePrescriptionTotalFee(items))
                .paid(prescription.getPaidAt() != null)
                .createdAt(prescription.getCreatedAt())
                .build();
    }

    private Registration getOwnedRegistration(Long registrationId, Long patientId) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "挂号记录不存在");
        }
        if (!registration.getPatientId().equals(patientId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return registration;
    }
}
