package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.enums.MedicalRecordStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.PaymentGuard;
import com.neusoft.cloudbrain.dto.request.medical.SaveMedicalRecordRequest;
import com.neusoft.cloudbrain.dto.response.medical.MedicalRecordVO;
import com.neusoft.cloudbrain.entity.MedicalRecord;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.MedicalRecordMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordMapper medicalRecordMapper;
    private final RegistrationMapper registrationMapper;
    private final UserMapper userMapper;

    @Transactional
    public MedicalRecordVO save(Long doctorId, SaveMedicalRecordRequest request) {
        Registration registration = registrationMapper.selectById(request.getRegistrationId());
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "挂号记录不存在");
        }
        assertDoctorOwnsRegistration(registration, doctorId);
        PaymentGuard.requireRegistrationPaid(registration);

        MedicalRecord record = medicalRecordMapper.selectOne(
                Wrappers.<MedicalRecord>lambdaQuery().eq(MedicalRecord::getRegistrationId, registration.getId())
        );
        if (record == null) {
            record = new MedicalRecord();
            record.setRegistrationId(registration.getId());
            record.setPatientId(registration.getPatientId());
            record.setDoctorId(registration.getDoctorId());
        }
        if (request.getDialogText() != null) {
            record.setDialogText(request.getDialogText());
        }
        applySaveFields(record, request);
        if (request.getIsAiGenerated() != null) {
            record.setIsAiGenerated(request.getIsAiGenerated());
        }
        record.setStatus(parseStatus(request.getStatus()));
        if (record.getId() == null) {
            medicalRecordMapper.insert(record);
        } else {
            medicalRecordMapper.updateById(record);
        }
        return toVO(record);
    }

    @Transactional
    public MedicalRecordVO confirm(Long id, Long doctorId) {
        MedicalRecord record = medicalRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "病历不存在");
        }
        Registration registration = registrationMapper.selectById(record.getRegistrationId());
        assertDoctorOwnsRegistration(registration, doctorId);
        PaymentGuard.requireRegistrationPaid(registration);
        record.setStatus(MedicalRecordStatus.CONFIRMED);
        record.setConfirmedAt(LocalDateTime.now());
        medicalRecordMapper.updateById(record);
        return toVO(record);
    }

    @Transactional(readOnly = true)
    public MedicalRecordVO getById(Long id) {
        MedicalRecord record = medicalRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "病历不存在");
        }
        return toVO(record);
    }

    @Transactional(readOnly = true)
    public MedicalRecordVO getByRegistration(Long registrationId, Long doctorId) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "挂号记录不存在");
        }
        assertDoctorOwnsRegistration(registration, doctorId);
        MedicalRecord record = medicalRecordMapper.selectOne(
                Wrappers.<MedicalRecord>lambdaQuery().eq(MedicalRecord::getRegistrationId, registrationId)
        );
        return record != null ? toVO(record) : null;
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordVO> listByPatient(Long patientId) {
        return medicalRecordMapper.selectList(
                Wrappers.<MedicalRecord>lambdaQuery()
                        .eq(MedicalRecord::getPatientId, patientId)
                        .orderByDesc(MedicalRecord::getCreatedAt)
        ).stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordVO> listByDoctor(Long doctorId, String patientName, LocalDate date) {
        return medicalRecordMapper.selectList(
                Wrappers.<MedicalRecord>lambdaQuery()
                        .eq(MedicalRecord::getDoctorId, doctorId)
                        .eq(MedicalRecord::getStatus, MedicalRecordStatus.CONFIRMED)
                        .orderByDesc(MedicalRecord::getConfirmedAt)
        ).stream()
                .map(this::toVO)
                .filter(vo -> matchesDoctorHistoryFilter(vo, patientName, date))
                .toList();
    }

    private boolean matchesDoctorHistoryFilter(MedicalRecordVO vo, String patientName, LocalDate date) {
        if (StringUtils.hasText(patientName)) {
            String keyword = patientName.trim();
            if (vo.getPatientName() == null || !vo.getPatientName().contains(keyword)) {
                return false;
            }
        }
        if (date != null) {
            LocalDateTime ref = vo.getConfirmedAt() != null ? vo.getConfirmedAt() : vo.getCreatedAt();
            if (ref == null || !ref.toLocalDate().equals(date)) {
                return false;
            }
        }
        return true;
    }

    private void assertDoctorOwnsRegistration(Registration registration, Long doctorId) {
        if (registration == null || !registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void applySaveFields(MedicalRecord record, SaveMedicalRecordRequest request) {
        if (request.getChiefComplaint() != null) {
            record.setChiefComplaint(request.getChiefComplaint());
        }
        if (request.getPresentIllness() != null) {
            record.setPresentIllness(request.getPresentIllness());
        }
        if (request.getPastHistory() != null) {
            record.setPastHistory(request.getPastHistory());
        }
        if (request.getPhysicalExam() != null) {
            record.setPhysicalExam(request.getPhysicalExam());
        }
        if (request.getDiagnosis() != null) {
            record.setDiagnosis(request.getDiagnosis());
        }
        if (request.getTreatmentPlan() != null) {
            record.setTreatmentPlan(request.getTreatmentPlan());
        }
        if (request.getAiDraft() != null) {
            record.setAiDraft(request.getAiDraft());
        }
    }

    private MedicalRecordStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return MedicalRecordStatus.DRAFT;
        }
        try {
            return MedicalRecordStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的病历状态: " + status);
        }
    }

    private MedicalRecordVO toVO(MedicalRecord record) {
        User patient = userMapper.selectById(record.getPatientId());
        User doctor = userMapper.selectById(record.getDoctorId());
        return MedicalRecordVO.builder()
                .id(record.getId())
                .registrationId(record.getRegistrationId())
                .patientName(patient != null ? patient.getRealName() : null)
                .doctorName(doctor != null ? doctor.getRealName() : null)
                .chiefComplaint(record.getChiefComplaint())
                .presentIllness(record.getPresentIllness())
                .pastHistory(record.getPastHistory())
                .dialogText(record.getDialogText())
                .physicalExam(record.getPhysicalExam())
                .diagnosis(record.getDiagnosis())
                .treatmentPlan(record.getTreatmentPlan())
                .isAiGenerated(record.getIsAiGenerated())
                .status(record.getStatus())
                .confirmedAt(record.getConfirmedAt())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
