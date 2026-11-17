package com.neusoft.cloudbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.neusoft.cloudbrain.common.enums.EquipmentStatus;
import com.neusoft.cloudbrain.entity.MedicalEquipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MedicalEquipmentMapper extends BaseMapper<MedicalEquipment> {

    List<MedicalEquipment> search(@Param("category") String category,
                                  @Param("status") EquipmentStatus status,
                                  @Param("keyword") String keyword);

    IPage<MedicalEquipment> searchPage(IPage<?> page,
                                       @Param("category") String category,
                                       @Param("status") EquipmentStatus status,
                                       @Param("keyword") String keyword);
}
