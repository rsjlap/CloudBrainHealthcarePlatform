package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.enums.ExamOrderStatus;
import com.neusoft.cloudbrain.common.enums.ExamOrderType;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.PaymentGuard;
import com.neusoft.cloudbrain.dto.request.exam.CreateExamOrderRequest;
import com.neusoft.cloudbrain.dto.response.exam.ExamOrderVO;
import com.neusoft.cloudbrain.entity.ExamItem;
import com.neusoft.cloudbrain.entity.ExamOrder;
import com.neusoft.cloudbrain.entity.LabItem;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.ExamOrderMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamOrderService {

    private final ExamOrderMapper examOrderMapper;
    private final RegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    private final ExamCatalogService examCatalogService;

    @Transactional
    public ExamOrderVO create(Long doctorId, CreateExamOrderRequest request) {
        Registration registration = registrationMapper.selectById(request.getRegistrationId());
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "挂号记录不存在");
        }
        PaymentGuard.requireRegistrationPaid(registration);
        User doctor = userMapper.selectById(doctorId);
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在");
        }

        ExamOrder order = new ExamOrder();
        order.setRegistrationId(registration.getId());
        order.setClinicalIndication(request.getClinicalIndication());
        order.setStatus(ExamOrderStatus.PENDING);
        order.setOrderedBy(doctorId);

        if (request.getLabItemId() != null || request.getExamItemId() != null || request.getOrderType() != null) {
            applyCatalogItem(order, request);
        } else if (StringUtils.hasText(request.getExamType())) {
            order.setOrderType(ExamOrderType.EXAM);
            order.setExamType(request.getExamType().trim());
            order.setBodyPart(request.getBodyPart());
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择检查/检验项目");
        }

        if (order.getFee() == null) {
            order.setFee(examCatalogService.resolveOrderFee(order));
        }

        examOrderMapper.insert(order);
        return toVO(order);
    }

    private void applyCatalogItem(ExamOrder order, CreateExamOrderRequest request) {
        ExamOrderType orderType = request.getOrderType();
        if (orderType == ExamOrderType.LAB || request.getLabItemId() != null) {
            if (request.getLabItemId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "检验项目不能为空");
            }
            LabItem item = examCatalogService.getEnabledLabItem(request.getLabItemId());
            if (item == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "检验项目不存在或已停用");
            }
            order.setOrderType(ExamOrderType.LAB);
            order.setLabItemId(item.getId());
            order.setExamItemId(null);
            order.setExamType(item.getItemName());
            order.setBodyPart(item.getCategory());
            order.setFee(item.getFee());
            return;
        }
        if (orderType == ExamOrderType.EXAM || request.getExamItemId() != null) {
            if (request.getExamItemId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "检查项目不能为空");
            }
            ExamItem item = examCatalogService.getEnabledExamItem(request.getExamItemId());
            if (item == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "检查项目不存在或已停用");
            }
            order.setOrderType(ExamOrderType.EXAM);
            order.setExamItemId(item.getId());
            order.setLabItemId(null);
            order.setExamType(item.getItemName());
            order.setBodyPart(item.getBodyPart());
            order.setFee(item.getFee());
            return;
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "无效的检查/检验类型");
    }

    @Transactional(readOnly = true)
    public List<ExamOrderVO> listPendingForInspector() {
        return examOrderMapper.selectList(
                Wrappers.<ExamOrder>lambdaQuery()
                        .in(ExamOrder::getStatus, ExamOrderStatus.PENDING, ExamOrderStatus.IN_PROGRESS)
                        .isNotNull(ExamOrder::getPaidAt)
        ).stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public List<ExamOrderVO> listByRegistration(Long registrationId) {
        return examOrderMapper.selectList(
                Wrappers.<ExamOrder>lambdaQuery().eq(ExamOrder::getRegistrationId, registrationId)
        ).stream().map(this::toVO).toList();
    }

    private ExamOrderVO toVO(ExamOrder order) {
        Registration registration = registrationMapper.selectById(order.getRegistrationId());
        User patient = registration != null ? userMapper.selectById(registration.getPatientId()) : null;
        User orderedBy = userMapper.selectById(order.getOrderedBy());
        return ExamOrderVO.builder()
                .id(order.getId())
                .registrationId(order.getRegistrationId())
                .patientName(patient != null ? patient.getRealName() : null)
                .orderType(order.getOrderType())
                .labItemId(order.getLabItemId())
                .examItemId(order.getExamItemId())
                .examType(order.getExamType())
                .bodyPart(order.getBodyPart())
                .clinicalIndication(order.getClinicalIndication())
                .fee(examCatalogService.resolveOrderFee(order))
                .status(order.getStatus())
                .orderedByName(orderedBy != null ? orderedBy.getRealName() : null)
                .paid(order.getPaidAt() != null)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
