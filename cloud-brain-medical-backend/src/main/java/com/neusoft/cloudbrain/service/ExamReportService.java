package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.enums.ExamOrderStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.PaymentGuard;
import com.neusoft.cloudbrain.dto.request.exam.SaveExamReportRequest;
import com.neusoft.cloudbrain.dto.response.ai.ExamReportDraftVO;
import com.neusoft.cloudbrain.dto.response.exam.ExamReportVO;
import com.neusoft.cloudbrain.dto.response.exam.InspectorExamHistoryVO;
import com.neusoft.cloudbrain.entity.ExamOrder;
import com.neusoft.cloudbrain.entity.ExamReport;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.ExamOrderMapper;
import com.neusoft.cloudbrain.mapper.ExamReportMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import com.neusoft.cloudbrain.service.ai.AiExamReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamReportService {

    private final ExamReportMapper examReportMapper;
    private final ExamOrderMapper examOrderMapper;
    private final RegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    private final AiExamReportService aiExamReportService;

    @Transactional
    public ExamReportVO generateAndSave(Long inspectorId, Long orderId) {
        ExamOrder order = requirePaidOrder(orderId);
        if (order.getStatus() == ExamOrderStatus.DONE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该检查申请已完成，无法重复生成");
        }
        User inspector = userMapper.selectById(inspectorId);
        if (inspector == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查医生不存在");
        }

        ExamReportDraftVO draft = aiExamReportService.generateExamReport(
                order.getExamType(), order.getBodyPart(), order.getClinicalIndication());

        ExamReport report = examReportMapper.selectOne(
                Wrappers.<ExamReport>lambdaQuery().eq(ExamReport::getOrderId, orderId)
        );
        if (report == null) {
            report = new ExamReport();
            report.setOrderId(orderId);
        }
        report.setReportText(draft.getReportText());
        report.setConclusion(draft.getConclusion());
        report.setInspectorId(inspectorId);
        report.setIsAiGenerated(1);
        report.setConfirmedAt(null);
        if (report.getId() == null) {
            examReportMapper.insert(report);
        } else {
            examReportMapper.updateById(report);
        }

        if (order.getStatus() == ExamOrderStatus.PENDING) {
            order.setStatus(ExamOrderStatus.IN_PROGRESS);
            examOrderMapper.updateById(order);
        }
        return toVO(report, order);
    }

    @Transactional
    public ExamReportVO save(Long inspectorId, SaveExamReportRequest request) {
        ExamOrder order = requirePaidOrder(request.getOrderId());
        User inspector = userMapper.selectById(inspectorId);
        if (inspector == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查医生不存在");
        }

        ExamReport report = examReportMapper.selectOne(
                Wrappers.<ExamReport>lambdaQuery().eq(ExamReport::getOrderId, request.getOrderId())
        );
        if (report == null) {
            report = new ExamReport();
            report.setOrderId(order.getId());
        }
        report.setReportText(request.getReportText());
        report.setConclusion(request.getConclusion());
        report.setImageUrls(request.getImageUrls());
        report.setInspectorId(inspectorId);
        if (report.getId() == null) {
            examReportMapper.insert(report);
        } else {
            examReportMapper.updateById(report);
        }
        return toVO(report, order);
    }

    @Transactional
    public ExamReportVO confirm(Long reportId) {
        ExamReport report = examReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查报告不存在");
        }
        ExamOrder order = examOrderMapper.selectById(report.getOrderId());
        PaymentGuard.requireExamOrderPaid(order);
        report.setConfirmedAt(LocalDateTime.now());
        examReportMapper.updateById(report);
        order.setStatus(ExamOrderStatus.DONE);
        examOrderMapper.updateById(order);
        return toVO(report, order);
    }

    @Transactional(readOnly = true)
    public ExamReportVO getByOrderId(Long orderId) {
        ExamReport report = examReportMapper.selectOne(
                Wrappers.<ExamReport>lambdaQuery().eq(ExamReport::getOrderId, orderId)
        );
        if (report == null) {
            return null;
        }
        ExamOrder order = examOrderMapper.selectById(orderId);
        return toVO(report, order);
    }

    @Transactional(readOnly = true)
    public List<InspectorExamHistoryVO> listHistoryForInspector(String patientName,
                                                                String doctorName,
                                                                LocalDate date) {
        List<ExamOrder> orders = examOrderMapper.selectList(
                Wrappers.<ExamOrder>lambdaQuery().eq(ExamOrder::getStatus, ExamOrderStatus.DONE)
        );
        return orders.stream()
                .map(this::toHistoryVO)
                .filter(vo -> vo != null)
                .filter(vo -> matchesHistoryFilter(vo, patientName, doctorName, date))
                .sorted(Comparator.comparing(
                        InspectorExamHistoryVO::getConfirmedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private InspectorExamHistoryVO toHistoryVO(ExamOrder order) {
        ExamReport report = examReportMapper.selectOne(
                Wrappers.<ExamReport>lambdaQuery().eq(ExamReport::getOrderId, order.getId())
        );
        if (report == null || report.getConfirmedAt() == null) {
            return null;
        }
        Registration registration = registrationMapper.selectById(order.getRegistrationId());
        User patient = registration != null ? userMapper.selectById(registration.getPatientId()) : null;
        User doctor = userMapper.selectById(order.getOrderedBy());
        User inspector = report.getInspectorId() != null
                ? userMapper.selectById(report.getInspectorId()) : null;
        return InspectorExamHistoryVO.builder()
                .orderId(order.getId())
                .reportId(report.getId())
                .registrationId(order.getRegistrationId())
                .patientName(patient != null ? patient.getRealName() : null)
                .doctorName(doctor != null ? doctor.getRealName() : null)
                .orderType(order.getOrderType())
                .examType(order.getExamType())
                .bodyPart(order.getBodyPart())
                .clinicalIndication(order.getClinicalIndication())
                .reportText(report.getReportText())
                .conclusion(report.getConclusion())
                .inspectorName(inspector != null ? inspector.getRealName() : null)
                .isAiGenerated(report.getIsAiGenerated())
                .orderCreatedAt(order.getCreatedAt())
                .confirmedAt(report.getConfirmedAt())
                .build();
    }

    private boolean matchesHistoryFilter(InspectorExamHistoryVO vo,
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
        if (date != null && vo.getConfirmedAt() != null
                && !vo.getConfirmedAt().toLocalDate().equals(date)) {
            return false;
        }
        return true;
    }

    private ExamOrder requirePaidOrder(Long orderId) {
        ExamOrder order = examOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "检查申请不存在");
        }
        PaymentGuard.requireExamOrderPaid(order);
        return order;
    }

    private ExamReportVO toVO(ExamReport report, ExamOrder order) {
        if (order == null) {
            order = examOrderMapper.selectById(report.getOrderId());
        }
        User inspector = report.getInspectorId() != null
                ? userMapper.selectById(report.getInspectorId()) : null;
        return ExamReportVO.builder()
                .id(report.getId())
                .orderId(report.getOrderId())
                .examType(order != null ? order.getExamType() : null)
                .reportText(report.getReportText())
                .conclusion(report.getConclusion())
                .imageUrls(report.getImageUrls())
                .isAiGenerated(report.getIsAiGenerated())
                .inspectorName(inspector != null ? inspector.getRealName() : null)
                .confirmedAt(report.getConfirmedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
