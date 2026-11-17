package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.neusoft.cloudbrain.common.enums.UserRole;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.util.PageUtil;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.DesensitizeUtil;
import com.neusoft.cloudbrain.dto.request.patient.UpdateAdminPatientRequest;
import com.neusoft.cloudbrain.dto.response.medical.MedicalRecordVO;
import com.neusoft.cloudbrain.dto.response.patient.AdminPatientDetailVO;
import com.neusoft.cloudbrain.dto.response.patient.AdminPatientListVO;
import com.neusoft.cloudbrain.dto.response.patient.AdminPatientStatsVO;
import com.neusoft.cloudbrain.dto.response.registration.RegistrationVO;
import com.neusoft.cloudbrain.entity.Patient;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.MedicalRecordMapper;
import com.neusoft.cloudbrain.mapper.PatientMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPatientService {

    private final PatientMapper patientMapper;
    private final UserMapper userMapper;
    private final RegistrationMapper registrationMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final RegistrationService registrationService;
    private final MedicalRecordService medicalRecordService;

    @Transactional(readOnly = true)
    public AdminPatientStatsVO getStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return AdminPatientStatsVO.builder()
                .totalCount(userMapper.selectCount(
                        Wrappers.<User>lambdaQuery().eq(User::getRole, UserRole.PATIENT)
                ))
                .maleCount(patientMapper.selectCount(
                        Wrappers.<Patient>lambdaQuery().eq(Patient::getGender, 1)
                ))
                .femaleCount(patientMapper.selectCount(
                        Wrappers.<Patient>lambdaQuery().eq(Patient::getGender, 2)
                ))
                .allergyCount(patientMapper.countWithAllergy())
                .todayNewCount(userMapper.selectCount(
                        Wrappers.<User>lambdaQuery()
                                .eq(User::getRole, UserRole.PATIENT)
                                .between(User::getCreatedAt, startOfDay, endOfDay)
                ))
                .visitedCount(patientMapper.countWithRegistration())
                .activeCount(userMapper.selectList(
                        Wrappers.<User>lambdaQuery().eq(User::getRole, UserRole.PATIENT).orderByAsc(User::getId)
                ).stream().filter(u -> u.getStatus() != null && u.getStatus() == 1).count())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResult<AdminPatientListVO> search(String keyword,
                                                 Integer gender,
                                                 Boolean hasAllergy,
                                                 LocalDate registerDateFrom,
                                                 LocalDate registerDateTo,
                                                 Long page,
                                                 Long size) {
        LocalDateTime startTime = registerDateFrom != null ? registerDateFrom.atStartOfDay() : null;
        LocalDateTime endTime = registerDateTo != null ? registerDateTo.atTime(LocalTime.MAX) : null;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        IPage<Patient> result = patientMapper.searchPage(
                PageUtil.of(page, size), normalizedKeyword, gender, hasAllergy, startTime, endTime);
        return PageResult.of(result, result.getRecords().stream().map(this::toListVO).toList());
    }

    @Transactional(readOnly = true)
    public AdminPatientDetailVO getDetail(Long patientId) {
        Patient patient = getPatient(patientId);
        User user = userMapper.selectById(patientId);
        List<RegistrationVO> registrations = registrationService.listByPatient(patientId);
        List<MedicalRecordVO> medicalRecords = medicalRecordService.listByPatient(patientId);
        return toDetailVO(patient, user, registrations, medicalRecords);
    }

    @Transactional
    public AdminPatientDetailVO update(Long patientId, UpdateAdminPatientRequest request) {
        Patient patient = getPatient(patientId);
        User user = userMapper.selectById(patientId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName().trim());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getGender() != null) {
            patient.setGender(request.getGender());
        }
        if (request.getBirthDate() != null) {
            patient.setBirthDate(request.getBirthDate());
        }
        if (request.getIdCard() != null) {
            patient.setIdCard(request.getIdCard());
        }
        if (request.getBloodType() != null) {
            patient.setBloodType(request.getBloodType());
        }
        if (request.getAllergyHistory() != null) {
            patient.setAllergyHistory(request.getAllergyHistory());
        }
        if (request.getMedicalHistory() != null) {
            patient.setMedicalHistory(request.getMedicalHistory());
        }
        if (request.getEmergencyContact() != null) {
            patient.setEmergencyContact(request.getEmergencyContact());
        }
        if (request.getEmergencyPhone() != null) {
            patient.setEmergencyPhone(request.getEmergencyPhone());
        }

        userMapper.updateById(user);
        patientMapper.updateById(patient);
        return getDetail(patientId);
    }

    private Patient getPatient(Long patientId) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "患者不存在");
        }
        return patient;
    }

    private AdminPatientListVO toListVO(Patient patient) {
        User user = userMapper.selectById(patient.getId());
        Long patientId = patient.getId();
        return AdminPatientListVO.builder()
                .id(patientId)
                .username(user != null ? user.getUsername() : null)
                .realName(user != null ? user.getRealName() : null)
                .phone(user != null ? DesensitizeUtil.maskPhone(user.getPhone()) : null)
                .gender(patient.getGender())
                .genderLabel(genderLabel(patient.getGender()))
                .allergyHistory(patient.getAllergyHistory())
                .status(user != null ? user.getStatus() : null)
                .registrationCount(registrationMapper.selectCount(
                        Wrappers.<com.neusoft.cloudbrain.entity.Registration>lambdaQuery()
                                .eq(com.neusoft.cloudbrain.entity.Registration::getPatientId, patientId)
                ))
                .medicalRecordCount(medicalRecordMapper.selectCount(
                        Wrappers.<com.neusoft.cloudbrain.entity.MedicalRecord>lambdaQuery()
                                .eq(com.neusoft.cloudbrain.entity.MedicalRecord::getPatientId, patientId)
                ))
                .createdAt(user != null ? user.getCreatedAt() : null)
                .build();
    }

    private AdminPatientDetailVO toDetailVO(Patient patient,
                                            User user,
                                            List<RegistrationVO> registrations,
                                            List<MedicalRecordVO> medicalRecords) {
        return AdminPatientDetailVO.builder()
                .id(patient.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .status(user.getStatus())
                .gender(patient.getGender())
                .genderLabel(genderLabel(patient.getGender()))
                .birthDate(patient.getBirthDate())
                .idCard(patient.getIdCard())
                .bloodType(patient.getBloodType())
                .allergyHistory(patient.getAllergyHistory())
                .medicalHistory(patient.getMedicalHistory())
                .emergencyContact(patient.getEmergencyContact())
                .emergencyPhone(patient.getEmergencyPhone())
                .createdAt(user.getCreatedAt())
                .registrations(registrations)
                .medicalRecords(medicalRecords)
                .build();
    }

    private String genderLabel(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }
}
