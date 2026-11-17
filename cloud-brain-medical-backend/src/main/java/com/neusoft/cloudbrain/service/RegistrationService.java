package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.enums.PrescriptionStatus;
import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.PaymentGuard;
import com.neusoft.cloudbrain.dto.request.registration.CreateRegistrationRequest;
import com.neusoft.cloudbrain.dto.response.registration.RegistrationPatientInfoVO;
import com.neusoft.cloudbrain.dto.response.registration.RegistrationVO;
import com.neusoft.cloudbrain.entity.*;
import com.neusoft.cloudbrain.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;
    private final ScheduleMapper scheduleMapper;
    private final PatientMapper patientMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final DepartmentMapper departmentMapper;

    @Transactional
    public RegistrationVO create(Long patientId, CreateRegistrationRequest request) {
        User patient = userMapper.selectById(patientId);
        if (patient == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "患者不存在");
        }
        Doctor doctor = doctorMapper.selectById(request.getDoctorId());
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在");
        }
        Schedule schedule = scheduleMapper.selectById(request.getScheduleId());
        if (schedule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "排班不存在");
        }

        if (schedule.getRegisteredCount() >= schedule.getMaxPatients()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "号源已满");
        }

        Registration registration = new Registration();
        registration.setRegNo(generateRegNo());
        registration.setPatientId(patientId);
        registration.setDoctorId(doctor.getId());
        registration.setDepartmentId(doctor.getDepartmentId());
        registration.setScheduleId(schedule.getId());
        registration.setChiefComplaint(request.getChiefComplaint());
        registration.setStatus(RegistrationStatus.PENDING);
        registration.setQueueNo(schedule.getRegisteredCount() + 1);
        registration.setVisitType(1);

        schedule.setRegisteredCount(schedule.getRegisteredCount() + 1);
        scheduleMapper.updateById(schedule);
        registrationMapper.insert(registration);
        return toVO(registration);
    }

    @Transactional(readOnly = true)
    public List<RegistrationVO> listByPatient(Long patientId) {
        return registrationMapper.selectList(
                Wrappers.<Registration>lambdaQuery()
                        .eq(Registration::getPatientId, patientId)
                        .orderByDesc(Registration::getCreatedAt)
        ).stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationVO> listQueueByDoctor(Long doctorId, LocalDate date) {
        return registrationMapper.findByDoctorAndWorkDateOrderByQueueNo(doctorId, date)
                .stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public RegistrationPatientInfoVO getPatientInfo(Long registrationId, Long doctorId) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "挂号记录不存在");
        }
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        Patient patient = patientMapper.selectById(registration.getPatientId());
        User patientUser = userMapper.selectById(registration.getPatientId());
        Schedule schedule = scheduleMapper.selectById(registration.getScheduleId());
        return RegistrationPatientInfoVO.builder()
                .registrationId(registration.getId())
                .patientId(registration.getPatientId())
                .patientName(patientUser != null ? patientUser.getRealName() : null)
                .regNo(registration.getRegNo())
                .workDate(schedule != null ? schedule.getWorkDate() : null)
                .queueNo(registration.getQueueNo())
                .status(registration.getStatus())
                .gender(patient != null ? patient.getGender() : null)
                .allergyHistory(patient != null ? patient.getAllergyHistory() : null)
                .medicalHistory(patient != null ? patient.getMedicalHistory() : null)
                .build();
    }

    @Transactional
    public RegistrationVO cancel(Long registrationId, Long patientId) {
        Registration registration = getOwnedRegistration(registrationId, patientId);
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不可取消");
        }
        registration.setStatus(RegistrationStatus.CANCELLED);
        Schedule schedule = scheduleMapper.selectById(registration.getScheduleId());
        if (schedule != null) {
            schedule.setRegisteredCount(Math.max(0, schedule.getRegisteredCount() - 1));
            scheduleMapper.updateById(schedule);
        }
        registrationMapper.updateById(registration);
        return toVO(registration);
    }

    @Transactional
    public RegistrationVO callNext(Long registrationId, Long doctorId) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "挂号记录不存在");
        }
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        PaymentGuard.requireRegistrationPaid(registration);
        registration.setStatus(RegistrationStatus.IN_PROGRESS);
        registrationMapper.updateById(registration);
        return toVO(registration);
    }

    @Transactional
    public RegistrationVO complete(Long registrationId, Long doctorId) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "挂号记录不存在");
        }
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        boolean hasUnpaidPrescription = prescriptionMapper.selectList(
                Wrappers.<Prescription>lambdaQuery().eq(Prescription::getRegistrationId, registrationId)
        ).stream().anyMatch(p -> p.getStatus() == PrescriptionStatus.PENDING_DISPENSE && p.getPaidAt() == null);
        if (hasUnpaidPrescription) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "患者尚有未缴纳的处方费，请先完成缴费");
        }
        registration.setStatus(RegistrationStatus.COMPLETED);
        registrationMapper.updateById(registration);
        return toVO(registration);
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

    private String generateRegNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = registrationMapper.selectCount(null) + 1;
        return "REG" + date + String.format("%04d", count);
    }

    public RegistrationVO toAdminVO(Registration registration) {
        return toVO(registration);
    }

    private RegistrationVO toVO(Registration registration) {
        User patient = userMapper.selectById(registration.getPatientId());
        User doctor = userMapper.selectById(registration.getDoctorId());
        Department department = departmentMapper.selectById(registration.getDepartmentId());
        Schedule schedule = scheduleMapper.selectById(registration.getScheduleId());
        return RegistrationVO.builder()
                .id(registration.getId())
                .regNo(registration.getRegNo())
                .patientId(registration.getPatientId())
                .patientName(patient != null ? patient.getRealName() : null)
                .doctorName(doctor != null ? doctor.getRealName() : null)
                .deptName(department != null ? department.getName() : null)
                .workDate(schedule != null ? schedule.getWorkDate() : null)
                .timeSlot(schedule != null ? schedule.getTimeSlot() : null)
                .status(registration.getStatus())
                .queueNo(registration.getQueueNo())
                .chiefComplaint(registration.getChiefComplaint())
                .createdAt(registration.getCreatedAt())
                .paid(registration.getPaidAt() != null)
                .build();
    }
}
