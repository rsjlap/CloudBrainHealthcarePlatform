package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.enums.PrescriptionStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.PaymentGuard;
import com.neusoft.cloudbrain.dto.request.prescription.CreatePrescriptionRequest;
import com.neusoft.cloudbrain.dto.response.prescription.PrescriptionItemVO;
import com.neusoft.cloudbrain.dto.response.prescription.PrescriptionVO;
import com.neusoft.cloudbrain.entity.*;
import com.neusoft.cloudbrain.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final RegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    private final DrugMapper drugMapper;
    private final DrugCatalogService drugCatalogService;

    @Transactional
    public PrescriptionVO create(Long doctorId, CreatePrescriptionRequest request) {
        Registration registration = registrationMapper.selectById(request.getRegistrationId());
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "挂号记录不存在");
        }
        PaymentGuard.requireRegistrationPaid(registration);
        User doctor = userMapper.selectById(doctorId);
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在");
        }

        Prescription prescription = new Prescription();
        prescription.setRegistrationId(registration.getId());
        prescription.setDoctorId(doctorId);
        prescription.setStatus(PrescriptionStatus.PENDING_DISPENSE);
        prescription.setRiskLevel(request.getRiskLevel());
        prescription.setAiReviewResult(request.getAiReviewResult());
        prescriptionMapper.insert(prescription);

        for (var itemReq : request.getItems()) {
            Drug drug = drugCatalogService.resolveForPrescription(itemReq);
            if (drug == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "请选择有效的药品目录项");
            }
            PrescriptionItem item = new PrescriptionItem();
            item.setPrescriptionId(prescription.getId());
            item.setDrugId(drug.getId());
            item.setDrugName(drug.getDrugName());
            item.setDrugCode(drug.getDrugCode());
            item.setDosage(itemReq.getDosage());
            item.setUnit(StringUtils.hasText(itemReq.getUnit()) ? itemReq.getUnit() : drug.getUnit());
            item.setFrequency(itemReq.getFrequency());
            item.setDurationDays(itemReq.getDurationDays());
            if (itemReq.getTotalAmount() != null) {
                item.setTotalAmount(itemReq.getTotalAmount());
            } else {
                item.setTotalAmount(BigDecimal.ONE);
            }
            prescriptionItemMapper.insert(item);
        }
        return toVO(prescription);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculatePrescriptionTotalFee(List<PrescriptionItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (PrescriptionItem item : items) {
            Drug drug = resolveDrugForItem(item);
            if (drug == null || drug.getUnitPrice() == null) {
                continue;
            }
            BigDecimal quantity = item.getTotalAmount();
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                quantity = BigDecimal.ONE;
            }
            total = total.add(drug.getUnitPrice().multiply(quantity));
        }
        return total;
    }

    @Transactional(readOnly = true)
    public List<PrescriptionVO> listByRegistration(Long registrationId) {
        return prescriptionMapper.selectList(
                Wrappers.<Prescription>lambdaQuery()
                        .eq(Prescription::getRegistrationId, registrationId)
                        .orderByDesc(Prescription::getCreatedAt)
        ).stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionVO> listByDoctor(Long doctorId) {
        return prescriptionMapper.selectList(
                Wrappers.<Prescription>lambdaQuery()
                        .eq(Prescription::getDoctorId, doctorId)
                        .orderByDesc(Prescription::getCreatedAt)
        ).stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionVO> listPendingForPharmacy() {
        return prescriptionMapper.selectList(
                Wrappers.<Prescription>lambdaQuery()
                        .eq(Prescription::getStatus, PrescriptionStatus.PENDING_DISPENSE)
                        .isNotNull(Prescription::getPaidAt)
        ).stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public PrescriptionVO getById(Long id) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "处方不存在");
        }
        return toVO(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionVO> listDispensedHistory(String patientName, String doctorName, LocalDate date) {
        return prescriptionMapper.selectList(
                Wrappers.<Prescription>lambdaQuery()
                        .eq(Prescription::getStatus, PrescriptionStatus.DISPENSED)
        ).stream()
                .map(this::toVO)
                .filter(vo -> matchesDispensedFilter(vo, patientName, doctorName, date))
                .sorted(Comparator.comparing(
                        PrescriptionVO::getDispensedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private boolean matchesDispensedFilter(PrescriptionVO vo,
                                           String patientName,
                                           String doctorName,
                                           LocalDate date) {
        if (StringUtils.hasText(patientName)) {
            String keyword = patientName.trim();
            if (vo.getPatientName() == null || !vo.getPatientName().contains(keyword)) {
                return false;
            }
        }
        if (StringUtils.hasText(doctorName)) {
            String keyword = doctorName.trim();
            if (vo.getDoctorName() == null || !vo.getDoctorName().contains(keyword)) {
                return false;
            }
        }
        if (date != null && vo.getDispensedAt() != null
                && !vo.getDispensedAt().toLocalDate().equals(date)) {
            return false;
        }
        return true;
    }

    @Transactional
    public PrescriptionVO dispense(Long prescriptionId, Long pharmacistId) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "处方不存在");
        }
        if (prescription.getStatus() != PrescriptionStatus.PENDING_DISPENSE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该处方已发药或状态无效");
        }
        PaymentGuard.requirePrescriptionPaid(prescription);
        User pharmacist = userMapper.selectById(pharmacistId);
        if (pharmacist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "药师不存在");
        }
        prescription.setStatus(PrescriptionStatus.DISPENSED);
        prescription.setDispensedBy(pharmacistId);
        prescription.setDispensedAt(LocalDateTime.now());
        List<PrescriptionItem> items = prescriptionItemMapper.selectList(
                Wrappers.<PrescriptionItem>lambdaQuery().eq(PrescriptionItem::getPrescriptionId, prescriptionId)
        );
        for (PrescriptionItem item : items) {
            Drug drug = resolveDrugForItem(item);
            if (drug == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, item.getDrugName() + " 不在药品目录或库存不足");
            }
            if (drug.getStockQuantity() == null || drug.getStockQuantity() <= 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, item.getDrugName() + " 库存不足");
            }
            drug.setStockQuantity(drug.getStockQuantity() - 1);
            drugMapper.updateById(drug);
        }
        prescriptionMapper.updateById(prescription);
        return toVO(prescription);
    }

    private PrescriptionVO toVO(Prescription prescription) {
        List<PrescriptionItemVO> items = prescriptionItemMapper.selectList(
                Wrappers.<PrescriptionItem>lambdaQuery().eq(PrescriptionItem::getPrescriptionId, prescription.getId())
        ).stream()
                .map(item -> PrescriptionItemVO.builder()
                        .id(item.getId())
                        .drugId(item.getDrugId())
                        .drugName(item.getDrugName())
                        .drugCode(item.getDrugCode())
                        .dosage(item.getDosage())
                        .unit(item.getUnit())
                        .frequency(item.getFrequency())
                        .durationDays(item.getDurationDays())
                        .totalAmount(item.getTotalAmount())
                        .build())
                .toList();

        Registration registration = registrationMapper.selectById(prescription.getRegistrationId());
        User patient = registration != null ? userMapper.selectById(registration.getPatientId()) : null;
        User doctor = userMapper.selectById(prescription.getDoctorId());
        User dispensedBy = prescription.getDispensedBy() != null
                ? userMapper.selectById(prescription.getDispensedBy()) : null;

        return PrescriptionVO.builder()
                .id(prescription.getId())
                .registrationId(prescription.getRegistrationId())
                .patientName(patient != null ? patient.getRealName() : null)
                .doctorName(doctor != null ? doctor.getRealName() : null)
                .status(prescription.getStatus())
                .aiReviewResult(prescription.getAiReviewResult())
                .riskLevel(prescription.getRiskLevel())
                .dispensedByName(dispensedBy != null ? dispensedBy.getRealName() : null)
                .dispensedAt(prescription.getDispensedAt())
                .paid(prescription.getPaidAt() != null)
                .createdAt(prescription.getCreatedAt())
                .items(items)
                .build();
    }

    private Drug resolveDrugForItem(PrescriptionItem item) {
        if (item.getDrugId() != null) {
            return drugCatalogService.getActiveDrug(item.getDrugId());
        }
        if (StringUtils.hasText(item.getDrugCode())) {
            return drugCatalogService.getActiveDrugByCode(item.getDrugCode());
        }
        return null;
    }
}
