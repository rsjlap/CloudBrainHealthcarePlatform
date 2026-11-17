package com.neusoft.cloudbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.cloudbrain.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {
}
