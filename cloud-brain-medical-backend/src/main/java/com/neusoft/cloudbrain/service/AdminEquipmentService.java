package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.enums.EquipmentStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.PageUtil;
import com.neusoft.cloudbrain.dto.request.equipment.CreateEquipmentUsageRequest;
import com.neusoft.cloudbrain.dto.request.equipment.SaveMedicalEquipmentRequest;
import com.neusoft.cloudbrain.dto.request.equipment.UpdateEquipmentStatusRequest;
import com.neusoft.cloudbrain.dto.response.equipment.EquipmentStatsVO;
import com.neusoft.cloudbrain.dto.response.equipment.EquipmentUsageRecordVO;
import com.neusoft.cloudbrain.dto.response.equipment.MedicalEquipmentVO;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.EquipmentUsageRecord;
import com.neusoft.cloudbrain.entity.MedicalEquipment;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.EquipmentUsageRecordMapper;
import com.neusoft.cloudbrain.mapper.MedicalEquipmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminEquipmentService {

    private final MedicalEquipmentMapper equipmentMapper;
    private final EquipmentUsageRecordMapper usageRecordMapper;
    private final DepartmentMapper departmentMapper;

    @Transactional(readOnly = true)
    public EquipmentStatsVO getStats() {
        long active = countByStatus(EquipmentStatus.ACTIVE);
        long disabled = countByStatus(EquipmentStatus.DISABLED);
        long maintenance = countByStatus(EquipmentStatus.MAINTENANCE);
        long fault = countByStatus(EquipmentStatus.FAULT);
        long total = equipmentMapper.selectCount(null);
        long abnormal = maintenance + fault + disabled;

        List<EquipmentStatsVO.StatusCountItem> distribution = Arrays.stream(EquipmentStatus.values())
                .map(status -> EquipmentStatsVO.StatusCountItem.builder()
                        .status(status.name())
                        .label(statusLabel(status))
                        .count(countByStatus(status))
                        .build())
                .toList();

        return EquipmentStatsVO.builder()
                .totalCount(total)
                .activeCount(active)
                .disabledCount(disabled)
                .maintenanceCount(maintenance)
                .faultCount(fault)
                .abnormalCount(abnormal)
                .statusDistribution(distribution)
                .build();
    }

    @Transactional(readOnly = true)
    public List<String> listCategoryNames() {
        return equipmentMapper.selectList(
                Wrappers.<MedicalEquipment>lambdaQuery().select(MedicalEquipment::getCategory)
        ).stream()
                .map(MedicalEquipment::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<MedicalEquipmentVO> listEquipment(String category, EquipmentStatus status, String keyword,
                                                        Long page, Long size) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String cat = StringUtils.hasText(category) ? category.trim() : null;
        IPage<MedicalEquipment> result = equipmentMapper.searchPage(
                PageUtil.of(page, size), cat, status, kw);
        return PageResult.of(result, result.getRecords().stream().map(this::toEquipmentVO).toList());
    }

    @Transactional(readOnly = true)
    public List<MedicalEquipmentVO> listEquipmentAll(String category, EquipmentStatus status, String keyword) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String cat = StringUtils.hasText(category) ? category.trim() : null;
        return equipmentMapper.search(cat, status, kw).stream()
                .map(this::toEquipmentVO)
                .toList();
    }

    @Transactional
    public MedicalEquipmentVO createEquipment(SaveMedicalEquipmentRequest request) {
        if (equipmentMapper.selectCount(
                Wrappers.<MedicalEquipment>lambdaQuery().eq(MedicalEquipment::getEquipmentCode, request.getEquipmentCode())
        ) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "设备编号已存在");
        }
        MedicalEquipment equipment = new MedicalEquipment();
        applyEquipment(equipment, request);
        equipment.setStatus(EquipmentStatus.ACTIVE);
        equipmentMapper.insert(equipment);
        return toEquipmentVO(equipment);
    }

    @Transactional
    public MedicalEquipmentVO updateEquipment(Long id, SaveMedicalEquipmentRequest request) {
        MedicalEquipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "设备不存在");
        }
        MedicalEquipment existing = equipmentMapper.selectOne(
                Wrappers.<MedicalEquipment>lambdaQuery().eq(MedicalEquipment::getEquipmentCode, request.getEquipmentCode())
        );
        if (existing != null && !existing.getId().equals(id)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "设备编号已存在");
        }
        applyEquipment(equipment, request);
        equipmentMapper.updateById(equipment);
        return toEquipmentVO(equipment);
    }

    @Transactional
    public MedicalEquipmentVO updateStatus(Long id, UpdateEquipmentStatusRequest request) {
        MedicalEquipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "设备不存在");
        }
        equipment.setStatus(request.getStatus());
        if (request.getStatus() == EquipmentStatus.MAINTENANCE) {
            equipment.setLastMaintenanceDate(LocalDate.now());
        }
        equipmentMapper.updateById(equipment);
        return toEquipmentVO(equipment);
    }

    @Transactional
    public void deleteEquipment(Long id) {
        MedicalEquipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "设备不存在");
        }
        usageRecordMapper.delete(
                Wrappers.<EquipmentUsageRecord>lambdaQuery().eq(EquipmentUsageRecord::getEquipmentId, id)
        );
        equipmentMapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<EquipmentUsageRecordVO> listUsageRecords(Long equipmentId) {
        MedicalEquipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "设备不存在");
        }
        return usageRecordMapper.selectList(
                Wrappers.<EquipmentUsageRecord>lambdaQuery()
                        .eq(EquipmentUsageRecord::getEquipmentId, equipmentId)
                        .orderByDesc(EquipmentUsageRecord::getStartTime)
        ).stream().map(record -> toUsageVO(record, equipment)).toList();
    }

    @Transactional
    public EquipmentUsageRecordVO createUsageRecord(Long equipmentId, CreateEquipmentUsageRequest request) {
        MedicalEquipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "设备不存在");
        }
        if (equipment.getStatus() != EquipmentStatus.ACTIVE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅运行中的设备可登记使用记录");
        }
        EquipmentUsageRecord record = new EquipmentUsageRecord();
        record.setEquipmentId(equipmentId);
        record.setUsageType(request.getUsageType());
        record.setOperatorName(request.getOperatorName());
        record.setPatientInfo(request.getPatientInfo());
        record.setStartTime(request.getStartTime());
        record.setEndTime(request.getEndTime());
        record.setRemark(request.getRemark());
        usageRecordMapper.insert(record);
        return toUsageVO(record, equipment);
    }

    private long countByStatus(EquipmentStatus status) {
        return equipmentMapper.selectCount(
                Wrappers.<MedicalEquipment>lambdaQuery().eq(MedicalEquipment::getStatus, status)
        );
    }

    private void applyEquipment(MedicalEquipment equipment, SaveMedicalEquipmentRequest request) {
        equipment.setEquipmentCode(request.getEquipmentCode());
        equipment.setEquipmentName(request.getEquipmentName());
        equipment.setCategory(request.getCategory().trim());
        if (request.getDepartmentId() != null) {
            Department department = departmentMapper.selectById(request.getDepartmentId());
            if (department == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "科室不存在");
            }
            equipment.setDepartmentId(department.getId());
        } else {
            equipment.setDepartmentId(null);
        }
        equipment.setModel(request.getModel());
        equipment.setManufacturer(request.getManufacturer());
        equipment.setLocation(request.getLocation());
        equipment.setPurchaseDate(request.getPurchaseDate());
        equipment.setLastMaintenanceDate(request.getLastMaintenanceDate());
        equipment.setRemark(request.getRemark());
    }

    private MedicalEquipmentVO toEquipmentVO(MedicalEquipment equipment) {
        Department department = equipment.getDepartmentId() != null
                ? departmentMapper.selectById(equipment.getDepartmentId()) : null;
        return MedicalEquipmentVO.builder()
                .id(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .category(equipment.getCategory())
                .departmentId(department != null ? department.getId() : null)
                .departmentName(department != null ? department.getName() : null)
                .model(equipment.getModel())
                .manufacturer(equipment.getManufacturer())
                .location(equipment.getLocation())
                .status(equipment.getStatus())
                .statusLabel(statusLabel(equipment.getStatus()))
                .purchaseDate(equipment.getPurchaseDate())
                .lastMaintenanceDate(equipment.getLastMaintenanceDate())
                .remark(equipment.getRemark())
                .updatedAt(equipment.getUpdatedAt())
                .build();
    }

    private EquipmentUsageRecordVO toUsageVO(EquipmentUsageRecord record, MedicalEquipment equipment) {
        return EquipmentUsageRecordVO.builder()
                .id(record.getId())
                .equipmentId(equipment.getId())
                .equipmentName(equipment.getEquipmentName())
                .usageType(record.getUsageType())
                .operatorName(record.getOperatorName())
                .patientInfo(record.getPatientInfo())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .remark(record.getRemark())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private String statusLabel(EquipmentStatus status) {
        return switch (status) {
            case ACTIVE -> "运行中";
            case DISABLED -> "已停用";
            case MAINTENANCE -> "维护中";
            case FAULT -> "故障";
        };
    }
}
