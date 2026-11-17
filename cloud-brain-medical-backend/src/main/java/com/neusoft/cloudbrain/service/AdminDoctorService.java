package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.enums.UserRole;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.DesensitizeUtil;
import com.neusoft.cloudbrain.common.util.PageUtil;
import com.neusoft.cloudbrain.common.util.PasswordUtil;
import com.neusoft.cloudbrain.dto.request.admin.SaveAdminDoctorRequest;
import com.neusoft.cloudbrain.dto.request.admin.UpdateAdminDoctorRequest;
import com.neusoft.cloudbrain.dto.response.admin.AdminDoctorListVO;
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

@Service
@RequiredArgsConstructor
public class AdminDoctorService {

    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordUtil passwordUtil;

    @Transactional(readOnly = true)
    public PageResult<AdminDoctorListVO> search(String keyword, Long departmentId, Integer status,
                                                Long page, Long size) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        IPage<AdminDoctorListVO> result = doctorMapper.adminSearchPage(
                PageUtil.of(page, size), normalizedKeyword, departmentId, status);
        result.getRecords().forEach(vo -> vo.setPhone(DesensitizeUtil.maskPhone(vo.getPhone())));
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public AdminDoctorListVO getDetail(Long id) {
        Doctor doctor = requireDoctor(id);
        User user = requireDoctorUser(id);
        return toVO(doctor, user);
    }

    @Transactional
    public AdminDoctorListVO create(SaveAdminDoctorRequest request) {
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名已存在");
        }
        requireDepartment(request.getDepartmentId());

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(passwordUtil.encode(request.getPassword()));
        user.setRole(UserRole.DOCTOR);
        user.setRealName(request.getRealName().trim());
        user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        user.setStatus(1);
        userMapper.insert(user);

        Doctor doctor = new Doctor();
        doctor.setId(user.getId());
        doctor.setDepartmentId(request.getDepartmentId());
        doctor.setTitle(trimToNull(request.getTitle()));
        doctor.setSpecialty(trimToNull(request.getSpecialty()));
        doctor.setIntroduction(trimToNull(request.getIntroduction()));
        doctor.setRegistrationFee(request.getRegistrationFee());
        doctor.setIsAvailable(1);
        doctorMapper.insert(doctor);
        return getDetail(user.getId());
    }

    @Transactional
    public AdminDoctorListVO update(Long id, UpdateAdminDoctorRequest request) {
        Doctor doctor = requireDoctor(id);
        User user = requireDoctorUser(id);

        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordUtil.encode(request.getPassword()));
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        if (request.getDepartmentId() != null) {
            requireDepartment(request.getDepartmentId());
            doctor.setDepartmentId(request.getDepartmentId());
        }
        if (request.getTitle() != null) {
            doctor.setTitle(trimToNull(request.getTitle()));
        }
        if (request.getSpecialty() != null) {
            doctor.setSpecialty(trimToNull(request.getSpecialty()));
        }
        if (request.getIntroduction() != null) {
            doctor.setIntroduction(trimToNull(request.getIntroduction()));
        }
        if (request.getRegistrationFee() != null) {
            doctor.setRegistrationFee(request.getRegistrationFee());
        }
        if (request.getIsAvailable() != null) {
            doctor.setIsAvailable(request.getIsAvailable());
        }

        userMapper.updateById(user);
        doctorMapper.updateById(doctor);
        return getDetail(id);
    }

    private Doctor requireDoctor(Long id) {
        Doctor doctor = doctorMapper.selectById(id);
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在");
        }
        return doctor;
    }

    private User requireDoctorUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getRole() != UserRole.DOCTOR) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生账号不存在");
        }
        return user;
    }

    private Department requireDepartment(Long departmentId) {
        Department department = departmentMapper.selectById(departmentId);
        if (department == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "科室不存在");
        }
        return department;
    }

    private AdminDoctorListVO toVO(Doctor doctor, User user) {
        Department department = doctor.getDepartmentId() != null
                ? departmentMapper.selectById(doctor.getDepartmentId()) : null;
        return AdminDoctorListVO.builder()
                .id(doctor.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(DesensitizeUtil.maskPhone(user.getPhone()))
                .status(user.getStatus())
                .departmentId(doctor.getDepartmentId())
                .departmentName(department != null ? department.getName() : null)
                .title(doctor.getTitle())
                .specialty(doctor.getSpecialty())
                .introduction(doctor.getIntroduction())
                .registrationFee(doctor.getRegistrationFee())
                .isAvailable(doctor.getIsAvailable())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
