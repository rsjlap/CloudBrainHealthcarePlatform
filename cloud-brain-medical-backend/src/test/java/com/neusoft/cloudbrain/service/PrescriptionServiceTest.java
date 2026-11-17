package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.PrescriptionStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.entity.Drug;
import com.neusoft.cloudbrain.entity.Prescription;
import com.neusoft.cloudbrain.entity.PrescriptionItem;
import com.neusoft.cloudbrain.mapper.DrugMapper;
import com.neusoft.cloudbrain.mapper.PrescriptionItemMapper;
import com.neusoft.cloudbrain.mapper.PrescriptionMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionService 处方费用与发药")
class PrescriptionServiceTest {

    @Mock
    private PrescriptionMapper prescriptionMapper;
    @Mock
    private PrescriptionItemMapper prescriptionItemMapper;
    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DrugMapper drugMapper;
    @Mock
    private DrugCatalogService drugCatalogService;

    private PrescriptionService prescriptionService;

    @BeforeEach
    void setUp() {
        prescriptionService = new PrescriptionService(
                prescriptionMapper,
                prescriptionItemMapper,
                registrationMapper,
                userMapper,
                drugMapper,
                drugCatalogService
        );
    }

    @Test
    @DisplayName("正常：按单价与数量计算处方总费用")
    void calculatePrescriptionTotalFee_success() {
        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId(1L);
        item.setTotalAmount(new BigDecimal("2"));

        Drug drug = new Drug();
        drug.setId(1L);
        drug.setUnitPrice(new BigDecimal("12.50"));

        when(drugCatalogService.getActiveDrug(1L)).thenReturn(drug);

        BigDecimal total = prescriptionService.calculatePrescriptionTotalFee(List.of(item));

        assertEquals(new BigDecimal("25.00"), total);
    }

    @Test
    @DisplayName("边界：数量为 null 或 0 时按 1 盒计")
    void calculatePrescriptionTotalFee_defaultQuantity() {
        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId(1L);
        item.setTotalAmount(null);

        Drug drug = new Drug();
        drug.setId(1L);
        drug.setUnitPrice(new BigDecimal("8.00"));
        when(drugCatalogService.getActiveDrug(1L)).thenReturn(drug);

        assertEquals(new BigDecimal("8.00"), prescriptionService.calculatePrescriptionTotalFee(List.of(item)));
    }

    @Test
    @DisplayName("边界：空列表费用为 0")
    void calculatePrescriptionTotalFee_empty() {
        assertEquals(BigDecimal.ZERO, prescriptionService.calculatePrescriptionTotalFee(Collections.emptyList()));
    }

    @Test
    @DisplayName("边界：药品无单价则跳过")
    void calculatePrescriptionTotalFee_missingPrice() {
        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId(1L);
        when(drugCatalogService.getActiveDrug(1L)).thenReturn(new Drug());

        assertEquals(BigDecimal.ZERO, prescriptionService.calculatePrescriptionTotalFee(List.of(item)));
    }

    @Test
    @DisplayName("异常：发药时处方未缴费")
    void dispense_unpaidPrescription() {
        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setStatus(PrescriptionStatus.PENDING_DISPENSE);
        prescription.setPaidAt(null);
        when(prescriptionMapper.selectById(1L)).thenReturn(prescription);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> prescriptionService.dispense(1L, 5L));
        assertEquals("患者尚未缴纳处方费，请先完成缴费", ex.getMessage());
    }

    @Test
    @DisplayName("异常：处方不存在")
    void dispense_notFound() {
        when(prescriptionMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> prescriptionService.dispense(1L, 5L));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：处方已发药不能重复发")
    void dispense_alreadyDispensed() {
        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setStatus(PrescriptionStatus.DISPENSED);
        prescription.setPaidAt(LocalDateTime.now());
        when(prescriptionMapper.selectById(1L)).thenReturn(prescription);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> prescriptionService.dispense(1L, 5L));
        assertEquals("该处方已发药或状态无效", ex.getMessage());
    }
}
