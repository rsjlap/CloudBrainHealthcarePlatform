package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.dto.response.drug.DrugVO;
import com.neusoft.cloudbrain.entity.Drug;
import com.neusoft.cloudbrain.mapper.DrugMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DrugService 药品查询")
class DrugServiceTest {

    @Mock
    private DrugMapper drugMapper;

    private DrugService drugService;

    @BeforeEach
    void setUp() {
        drugService = new DrugService(drugMapper);
    }

    @Test
    @DisplayName("正常：返回在售药品列�?)
    void listActive_returnsActiveDrugs() {
        Drug drug = new Drug();
        drug.setId(1L);
        drug.setDrugCode("D001");
        drug.setDrugName("阿莫西林");
        drug.setStatus(1);
        drug.setUnitPrice(new BigDecimal("12.50"));
        when(drugMapper.selectList(any())).thenReturn(List.of(drug));

        List<DrugVO> result = drugService.listActive();

        assertEquals(1, result.size());
        assertEquals("阿莫西林", result.get(0).getDrugName());
        assertEquals("D001", result.get(0).getDrugCode());
    }
}
