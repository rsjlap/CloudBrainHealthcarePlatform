package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentService 科室查询")
class DepartmentServiceTest {

    @Mock
    private DepartmentMapper departmentMapper;

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentMapper);
    }

    @Test
    @DisplayName("正常：列出全部科�?)
    void listAll_returnsDepartments() {
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("内科");
        when(departmentMapper.selectList(any())).thenReturn(List.of(dept));

        List<Department> result = departmentService.listAll();

        assertEquals(1, result.size());
        assertEquals("内科", result.get(0).getName());
    }

    @Test
    @DisplayName("正常：按 ID 查询科室")
    void getById_found() {
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("外科");
        when(departmentMapper.selectById(1L)).thenReturn(dept);

        Department result = departmentService.getById(1L);

        assertEquals("外科", result.getName());
    }

    @Test
    @DisplayName("异常：科室不存在")
    void getById_notFound() {
        when(departmentMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> departmentService.getById(99L));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
    }
}
