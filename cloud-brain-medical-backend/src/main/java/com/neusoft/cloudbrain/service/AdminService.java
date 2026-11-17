package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import com.neusoft.cloudbrain.common.enums.UserRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.util.PageUtil;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.PasswordUtil;
import com.neusoft.cloudbrain.common.util.DesensitizeUtil;
import com.neusoft.cloudbrain.dto.request.admin.UpdateAdminStaffRequest;
import com.neusoft.cloudbrain.dto.response.admin.AdminDashboardVO;
import com.neusoft.cloudbrain.dto.response.admin.AdminUserVO;
import com.neusoft.cloudbrain.dto.response.admin.ChartCountItemVO;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    private final DepartmentMapper departmentMapper;

    @Transactional(readOnly = true)
    public AdminDashboardVO getDashboardStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return AdminDashboardVO.builder()
                .todayRegistrations(registrationMapper.selectCount(
                        Wrappers.<com.neusoft.cloudbrain.entity.Registration>lambdaQuery()
                                .between(com.neusoft.cloudbrain.entity.Registration::getCreatedAt, startOfDay, endOfDay)
                ))
                .totalRegistrations(registrationMapper.selectCount(null))
                .inProgressCount(registrationMapper.selectCount(
                        Wrappers.<com.neusoft.cloudbrain.entity.Registration>lambdaQuery()
                                .eq(com.neusoft.cloudbrain.entity.Registration::getStatus, RegistrationStatus.IN_PROGRESS)
                ))
                .completedCount(registrationMapper.selectCount(
                        Wrappers.<com.neusoft.cloudbrain.entity.Registration>lambdaQuery()
                                .eq(com.neusoft.cloudbrain.entity.Registration::getStatus, RegistrationStatus.COMPLETED)
                ))
                .patientCount(userMapper.selectCount(
                        Wrappers.<User>lambdaQuery().eq(User::getRole, UserRole.PATIENT)
                ))
                .doctorCount(userMapper.selectCount(
                        Wrappers.<User>lambdaQuery().eq(User::getRole, UserRole.DOCTOR)
                ))
                .registrationTrend(buildRegistrationTrend())
                .departmentDistribution(buildDepartmentDistribution())
                .build();
    }

    private List<ChartCountItemVO> buildRegistrationTrend() {
        List<ChartCountItemVO> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            long count = registrationMapper.selectCount(
                    Wrappers.<com.neusoft.cloudbrain.entity.Registration>lambdaQuery()
                            .between(com.neusoft.cloudbrain.entity.Registration::getCreatedAt, start, end)
            );
            trend.add(ChartCountItemVO.builder()
                    .label(date.toString())
                    .count(count)
                    .build());
        }
        return trend;
    }

    private List<ChartCountItemVO> buildDepartmentDistribution() {
        List<Department> departments = departmentMapper.selectList(
                Wrappers.<Department>lambdaQuery().orderByAsc(Department::getSortOrder)
        );
        return departments.stream()
                .map(dept -> ChartCountItemVO.builder()
                        .label(dept.getName())
                        .count(registrationMapper.selectCount(
                                Wrappers.<com.neusoft.cloudbrain.entity.Registration>lambdaQuery()
                                        .eq(com.neusoft.cloudbrain.entity.Registration::getDepartmentId, dept.getId())
                        ))
                        .build())
                .filter(item -> item.getCount() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserVO> listUsers() {
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery().orderByAsc(User::getId)
        ).stream().map(this::toUserVO).toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AdminUserVO> listStaff(UserRole role, String keyword, Long page, Long size) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        IPage<User> userPage = userMapper.selectPage(
                PageUtil.of(page, size),
                Wrappers.<User>lambdaQuery()
                        .eq(role != null && role != UserRole.PATIENT, User::getRole, role)
                        .in(role == null, User::getRole,
                                EnumSet.of(UserRole.ADMIN, UserRole.DOCTOR, UserRole.INSPECTOR, UserRole.PHARMACIST))
                        .and(normalizedKeyword != null, wrapper -> wrapper
                                .like(User::getRealName, normalizedKeyword)
                                .or()
                                .like(User::getUsername, normalizedKeyword)
                                .or()
                                .like(User::getPhone, normalizedKeyword))
                        .orderByAsc(User::getId)
        );
        if (role == UserRole.PATIENT) {
            return PageResult.<AdminUserVO>builder()
                    .records(List.of())
                    .total(0)
                    .page(PageUtil.normalizePage(page))
                    .size(PageUtil.normalizeSize(size))
                    .build();
        }
        List<AdminUserVO> records = userPage.getRecords().stream().map(this::toUserVO).toList();
        return PageResult.of(userPage, records);
    }

    @Transactional(readOnly = true)
    public List<AdminUserVO> listStaff(UserRole role, String keyword) {
        List<User> users;
        if (role != null) {
            if (role == UserRole.PATIENT) {
                return List.of();
            }
            users = userMapper.selectList(
                    Wrappers.<User>lambdaQuery().eq(User::getRole, role).orderByAsc(User::getId)
            );
        } else {
            users = userMapper.selectList(
                    Wrappers.<User>lambdaQuery()
                            .in(User::getRole, EnumSet.of(UserRole.ADMIN, UserRole.DOCTOR, UserRole.INSPECTOR, UserRole.PHARMACIST))
                            .orderByAsc(User::getId)
            );
        }
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return users.stream()
                .filter(user -> normalizedKeyword == null
                        || user.getRealName().toLowerCase().contains(normalizedKeyword)
                        || user.getUsername().toLowerCase().contains(normalizedKeyword)
                        || (user.getPhone() != null && user.getPhone().contains(normalizedKeyword)))
                .map(this::toUserVO)
                .toList();
    }

    @Transactional
    public AdminUserVO updateStaff(Long id, UpdateAdminStaffRequest request) {
        User user = userMapper.selectById(id);
        if (user == null || user.getRole() == UserRole.PATIENT) {
            throw new BusinessException(ResultCode.NOT_FOUND, "工作人员不存在");
        }
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
        userMapper.updateById(user);
        return toUserVO(user);
    }

    private AdminUserVO toUserVO(User user) {
        return AdminUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(DesensitizeUtil.maskPhone(user.getPhone()))
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
