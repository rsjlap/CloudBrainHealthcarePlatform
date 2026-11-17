package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.dto.response.drug.DrugVO;
import com.neusoft.cloudbrain.entity.Drug;
import com.neusoft.cloudbrain.mapper.DrugMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DrugService {

    private final DrugMapper drugMapper;

    @Transactional(readOnly = true)
    public List<DrugVO> listActive() {
        return drugMapper.selectList(
                Wrappers.<Drug>lambdaQuery().eq(Drug::getStatus, 1)
        ).stream().map(this::toVO).toList();
    }

    private DrugVO toVO(Drug drug) {
        return DrugVO.builder()
                .id(drug.getId())
                .drugCode(drug.getDrugCode())
                .drugName(drug.getDrugName())
                .specification(drug.getSpecification())
                .unit(drug.getUnit())
                .unitPrice(drug.getUnitPrice())
                .manufacturer(drug.getManufacturer())
                .contraindication(drug.getContraindication())
                .build();
    }
}
