package com.neusoft.cloudbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.neusoft.cloudbrain.dto.response.admin.AdminDoctorListVO;
import com.neusoft.cloudbrain.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {

    List<Doctor> findAvailableByDepartmentAndDate(@Param("deptId") Long deptId,
                                                  @Param("workDate") LocalDate workDate);

    IPage<AdminDoctorListVO> adminSearchPage(IPage<?> page,
                                             @Param("keyword") String keyword,
                                             @Param("departmentId") Long departmentId,
                                             @Param("status") Integer status);
}
