package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.request.admin.SaveAdminDrugRequest;
import com.neusoft.cloudbrain.dto.request.admin.UpdateAdminDrugRequest;
import com.neusoft.cloudbrain.dto.response.admin.AdminDrugVO;
import com.neusoft.cloudbrain.entity.Drug;
import com.neusoft.cloudbrain.mapper.DrugMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDrugService 药品管理")
class AdminDrugServiceTest {

    @Mock
    private DrugMapper drugMapper;

    private AdminDrugService adminDrugService;

    @BeforeEach
    void setUp() {
        adminDrugService = new AdminDrugService(drugMapper);
    }

    @Test
    @DisplayName("正常：分页搜索药�?)
    void search_returnsPage() {
        Drug drug = buildDrug(1L, "D001", "阿莫西林");
        Page<Drug> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(drug));
        page.setTotal(1);
        when(drugMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<AdminDrugVO> result = adminDrugService.search("阿莫", 1, 1L, 10L);

        assertEquals(1, result.getRecords().size());
        assertEquals("阿莫西林", result.getRecords().get(0).getDrugName());
    }

    @Test
    @DisplayName("正常：创建药�?)
    void create_success() {
        when(drugMapper.selectOne(any())).thenReturn(null);

        SaveAdminDrugRequest request = new SaveAdminDrugRequest();
        request.setDrugCode("D002");
        request.setDrugName("布洛�?);
        request.setUnitPrice(new BigDecimal("8.00"));

        AdminDrugVO vo = adminDrugService.create(request);

        verify(drugMapper).insert(any(Drug.class));
        assertEquals("布洛�?, vo.getDrugName());
        assertEquals(1, vo.getStatus());
    }

    @Test
    @DisplayName("异常：药品编码重�?)
    void create_duplicateCode() {
        when(drugMapper.selectOne(any())).thenReturn(buildDrug(1L, "D001", "已有"));

        SaveAdminDrugRequest request = new SaveAdminDrugRequest();
        request.setDrugCode("D001");
        request.setDrugName("新药");

        BusinessException ex = assertThrows(BusinessException.class, () -> adminDrugService.create(request));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：药品不存在")
    void getDetail_notFound() {
        when(drugMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> adminDrugService.getDetail(99L));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
    }

    @Test
    @DisplayName("正常：更新药�?)
    void update_success() {
        Drug drug = buildDrug(1L, "D001", "旧名");
        when(drugMapper.selectById(1L)).thenReturn(drug);
        when(drugMapper.selectOne(any())).thenReturn(null);

        UpdateAdminDrugRequest request = new UpdateAdminDrugRequest();
        request.setDrugName("新名");

        AdminDrugVO vo = adminDrugService.update(1L, request);

        verify(drugMapper).updateById(drug);
        assertEquals("新名", vo.getDrugName());
    }

    private static Drug buildDrug(Long id, String code, String name) {
        Drug drug = new Drug();
        drug.setId(id);
        drug.setDrugCode(code);
        drug.setDrugName(name);
        drug.setStatus(1);
        drug.setStockQuantity(100);
        return drug;
    }
}
