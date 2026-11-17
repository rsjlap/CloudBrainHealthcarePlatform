package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.EquipmentStatus;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.request.equipment.SaveMedicalEquipmentRequest;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.MedicalEquipment;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.EquipmentUsageRecordMapper;
import com.neusoft.cloudbrain.mapper.MedicalEquipmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminEquipmentService 设备管理")
class AdminEquipmentServiceTest {

    @Mock
    private MedicalEquipmentMapper equipmentMapper;
    @Mock
    private EquipmentUsageRecordMapper usageRecordMapper;
    @Mock
    private DepartmentMapper departmentMapper;

    private AdminEquipmentService adminEquipmentService;

    @BeforeEach
    void setUp() {
        adminEquipmentService = new AdminEquipmentService(equipmentMapper, usageRecordMapper, departmentMapper);
    }

    @Test
    @DisplayName("正常：设备统�?)
    void getStats() {
        when(equipmentMapper.selectCount(any())).thenReturn(10L);
        when(equipmentMapper.selectCount(any())).thenReturn(10L, 7L, 1L, 1L, 1L);

        var stats = adminEquipmentService.getStats();

        assertEquals(10L, stats.getTotalCount());
        assertEquals(4, stats.getStatusDistribution().size());
    }

    @Test
    @DisplayName("异常：设备编号重�?)
    void createEquipment_duplicateCode() {
        when(equipmentMapper.selectCount(any())).thenReturn(1L);

        SaveMedicalEquipmentRequest request = new SaveMedicalEquipmentRequest();
        request.setEquipmentCode("EQ001");
        request.setEquipmentName("CT�?);
        request.setCategory("影像");
        request.setDepartmentId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminEquipmentService.createEquipment(request));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("正常：创建设�?)
    void createEquipment_success() {
        when(equipmentMapper.selectCount(any())).thenReturn(0L);
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("放射�?);
        when(departmentMapper.selectById(1L)).thenReturn(dept);

        SaveMedicalEquipmentRequest request = new SaveMedicalEquipmentRequest();
        request.setEquipmentCode("EQ002");
        request.setEquipmentName("MRI");
        request.setCategory("影像");
        request.setDepartmentId(1L);

        var vo = adminEquipmentService.createEquipment(request);

        verify(equipmentMapper).insert(any(MedicalEquipment.class));
        assertEquals("MRI", vo.getEquipmentName());
        assertEquals(EquipmentStatus.ACTIVE, vo.getStatus());
    }

    @Test
    @DisplayName("异常：设备不存在")
    void updateEquipment_notFound() {
        when(equipmentMapper.selectById(99L)).thenReturn(null);

        SaveMedicalEquipmentRequest request = new SaveMedicalEquipmentRequest();
        request.setEquipmentCode("EQ003");
        request.setEquipmentName("X光机");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminEquipmentService.updateEquipment(99L, request));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
    }
}
