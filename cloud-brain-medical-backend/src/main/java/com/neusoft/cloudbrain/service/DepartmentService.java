package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentMapper departmentMapper;

    public List<Department> listAll() {
        return departmentMapper.selectList(
                Wrappers.<Department>lambdaQuery().orderByAsc(Department::getSortOrder)
        );
    }

    public Department getById(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "科室不存在");
        }
        return department;
    }
}
