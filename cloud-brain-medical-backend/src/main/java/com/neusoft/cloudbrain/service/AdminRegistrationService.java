package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.PageUtil;
import com.neusoft.cloudbrain.dto.response.registration.RegistrationVO;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class AdminRegistrationService {

    private final RegistrationMapper registrationMapper;
    private final RegistrationService registrationService;

    @Transactional(readOnly = true)
    public PageResult<RegistrationVO> search(String keyword,
                                             RegistrationStatus status,
                                             Long departmentId,
                                             Long doctorId,
                                             Boolean paid,
                                             LocalDate dateFrom,
                                             LocalDate dateTo,
                                             Long page,
                                             Long size) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        LocalDateTime startTime = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime endTime = dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;
        IPage<Registration> result = registrationMapper.adminSearchPage(
                PageUtil.of(page, size),
                normalizedKeyword,
                status,
                departmentId,
                doctorId,
                paid,
                startTime,
                endTime
        );
        return PageResult.of(result, result.getRecords().stream()
                .map(registrationService::toAdminVO)
                .toList());
    }

    @Transactional(readOnly = true)
    public RegistrationVO getDetail(Long id) {
        Registration registration = registrationMapper.selectById(id);
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "挂号记录不存在");
        }
        return registrationService.toAdminVO(registration);
    }
}
