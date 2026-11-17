package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.request.doctor.UpdateDoctorProfileRequest;
import com.neusoft.cloudbrain.dto.response.doctor.DoctorProfileVO;
import com.neusoft.cloudbrain.dto.response.registration.DoctorCardVO;
import com.neusoft.cloudbrain.dto.response.registration.ScheduleVO;
import com.neusoft.cloudbrain.common.util.DesensitizeUtil;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.Doctor;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.DoctorMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final ScheduleService scheduleService;

    public List<DoctorCardVO> listByDepartment(Long deptId, LocalDate date) {
        List<Doctor> doctors = date != null
                ? doctorMapper.findAvailableByDepartmentAndDate(deptId, date)
                : doctorMapper.selectList(
                        Wrappers.<Doctor>lambdaQuery().eq(Doctor::getDepartmentId, deptId)
                );
        return doctors.stream().map(this::toCardVO).toList();
    }

    public DoctorCardVO getById(Long id) {
        Doctor doctor = doctorMapper.selectById(id);
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在");
        }
        return toCardVO(doctor);
    }

    @Transactional(readOnly = true)
    public DoctorProfileVO getProfile(Long userId) {
        Doctor doctor = requireDoctor(userId);
        User user = userMapper.selectById(userId);
        Department department = doctor.getDepartmentId() != null
                ? departmentMapper.selectById(doctor.getDepartmentId()) : null;
        return DoctorProfileVO.builder()
                .userId(userId)
                .username(user != null ? user.getUsername() : null)
                .realName(user != null ? user.getRealName() : null)
                .phone(user != null ? DesensitizeUtil.maskPhone(user.getPhone()) : null)
                .title(doctor.getTitle())
                .departmentName(department != null ? department.getName() : null)
                .specialty(doctor.getSpecialty())
                .introduction(doctor.getIntroduction())
                .registrationFee(doctor.getRegistrationFee())
                .build();
    }

    @Transactional
    public DoctorProfileVO updateProfile(Long userId, UpdateDoctorProfileRequest request) {
        Doctor doctor = requireDoctor(userId);
        if (request.getSpecialty() != null) {
            doctor.setSpecialty(StringUtils.hasText(request.getSpecialty()) ? request.getSpecialty().trim() : null);
        }
        if (request.getIntroduction() != null) {
            doctor.setIntroduction(StringUtils.hasText(request.getIntroduction()) ? request.getIntroduction().trim() : null);
        }
        doctorMapper.updateById(doctor);
        return getProfile(userId);
    }

    @Transactional(readOnly = true)
    public List<ScheduleVO> listMySchedules(Long userId, LocalDate startDate, LocalDate endDate) {
        requireDoctor(userId);
        return scheduleService.listByDoctorInRange(userId, startDate, endDate);
    }

    private Doctor requireDoctor(Long userId) {
        Doctor doctor = doctorMapper.selectById(userId);
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生档案不存在");
        }
        return doctor;
    }

    private DoctorCardVO toCardVO(Doctor doctor) {
        User user = userMapper.selectById(doctor.getId());
        Department department = doctor.getDepartmentId() != null
                ? departmentMapper.selectById(doctor.getDepartmentId()) : null;
        return DoctorCardVO.builder()
                .id(doctor.getId())
                .realName(user != null ? user.getRealName() : null)
                .title(doctor.getTitle())
                .specialty(doctor.getSpecialty())
                .registrationFee(doctor.getRegistrationFee())
                .departmentName(department != null ? department.getName() : null)
                .scheduleSlots(Collections.emptyList())
                .build();
    }
}
