package com.neusoft.cloudbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.neusoft.cloudbrain.entity.Patient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PatientMapper extends BaseMapper<Patient> {

    long countWithAllergy();

    long countWithRegistration();

    List<Patient> search(@Param("keyword") String keyword,
                         @Param("gender") Integer gender,
                         @Param("hasAllergy") Boolean hasAllergy,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    IPage<Patient> searchPage(IPage<?> page,
                              @Param("keyword") String keyword,
                              @Param("gender") Integer gender,
                              @Param("hasAllergy") Boolean hasAllergy,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);
}
