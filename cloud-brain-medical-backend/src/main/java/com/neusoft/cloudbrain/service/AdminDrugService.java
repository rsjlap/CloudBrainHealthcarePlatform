package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.PageUtil;
import com.neusoft.cloudbrain.dto.request.admin.SaveAdminDrugRequest;
import com.neusoft.cloudbrain.dto.request.admin.UpdateAdminDrugRequest;
import com.neusoft.cloudbrain.dto.response.admin.AdminDrugVO;
import com.neusoft.cloudbrain.entity.Drug;
import com.neusoft.cloudbrain.mapper.DrugMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminDrugService {

    private final DrugMapper drugMapper;

    @Transactional(readOnly = true)
    public PageResult<AdminDrugVO> search(String keyword, Integer status, Long page, Long size) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        IPage<Drug> result = drugMapper.selectPage(
                PageUtil.of(page, size),
                Wrappers.<Drug>lambdaQuery()
                        .eq(status != null, Drug::getStatus, status)
                        .and(normalizedKeyword != null, wrapper -> wrapper
                                .like(Drug::getDrugName, normalizedKeyword)
                                .or()
                                .like(Drug::getDrugCode, normalizedKeyword)
                                .or()
                                .like(Drug::getManufacturer, normalizedKeyword))
                        .orderByDesc(Drug::getId)
        );
        return PageResult.of(result, result.getRecords().stream().map(this::toVO).toList());
    }

    @Transactional(readOnly = true)
    public AdminDrugVO getDetail(Long id) {
        return toVO(requireDrug(id));
    }

    @Transactional
    public AdminDrugVO create(SaveAdminDrugRequest request) {
        assertUniqueCode(request.getDrugCode(), null);
        Drug drug = new Drug();
        applyDrug(drug, request);
        drugMapper.insert(drug);
        return toVO(drug);
    }

    @Transactional
    public AdminDrugVO update(Long id, UpdateAdminDrugRequest request) {
        Drug drug = requireDrug(id);
        if (StringUtils.hasText(request.getDrugCode())) {
            assertUniqueCode(request.getDrugCode(), id);
            drug.setDrugCode(request.getDrugCode().trim());
        }
        if (StringUtils.hasText(request.getDrugName())) {
            drug.setDrugName(request.getDrugName().trim());
        }
        if (request.getSpecification() != null) {
            drug.setSpecification(trimToNull(request.getSpecification()));
        }
        if (request.getUnit() != null) {
            drug.setUnit(trimToNull(request.getUnit()));
        }
        if (request.getUnitPrice() != null) {
            drug.setUnitPrice(request.getUnitPrice());
        }
        if (request.getManufacturer() != null) {
            drug.setManufacturer(trimToNull(request.getManufacturer()));
        }
        if (request.getContraindication() != null) {
            drug.setContraindication(trimToNull(request.getContraindication()));
        }
        if (request.getStatus() != null) {
            drug.setStatus(request.getStatus());
        }
        if (request.getStockQuantity() != null) {
            drug.setStockQuantity(request.getStockQuantity());
        }
        drugMapper.updateById(drug);
        return toVO(drug);
    }

    private void applyDrug(Drug drug, SaveAdminDrugRequest request) {
        drug.setDrugCode(request.getDrugCode().trim());
        drug.setDrugName(request.getDrugName().trim());
        drug.setSpecification(trimToNull(request.getSpecification()));
        drug.setUnit(trimToNull(request.getUnit()));
        drug.setUnitPrice(request.getUnitPrice());
        drug.setManufacturer(trimToNull(request.getManufacturer()));
        drug.setContraindication(trimToNull(request.getContraindication()));
        drug.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        drug.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 100);
    }

    private Drug requireDrug(Long id) {
        Drug drug = drugMapper.selectById(id);
        if (drug == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "药品不存在");
        }
        return drug;
    }

    private void assertUniqueCode(String drugCode, Long excludeId) {
        Drug existing = drugMapper.selectOne(
                Wrappers.<Drug>lambdaQuery().eq(Drug::getDrugCode, drugCode.trim())
        );
        if (existing != null && (excludeId == null || !existing.getId().equals(excludeId))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "药品编码已存在");
        }
    }

    private AdminDrugVO toVO(Drug drug) {
        return AdminDrugVO.builder()
                .id(drug.getId())
                .drugCode(drug.getDrugCode())
                .drugName(drug.getDrugName())
                .specification(drug.getSpecification())
                .unit(drug.getUnit())
                .unitPrice(drug.getUnitPrice())
                .manufacturer(drug.getManufacturer())
                .contraindication(drug.getContraindication())
                .status(drug.getStatus())
                .stockQuantity(drug.getStockQuantity())
                .build();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
