package com.neusoft.cloudbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import com.neusoft.cloudbrain.entity.Registration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    List<Registration> findByDoctorAndWorkDateOrderByQueueNo(@Param("doctorId") Long doctorId,
                                                             @Param("workDate") LocalDate workDate);

    IPage<Registration> adminSearchPage(IPage<?> page,
                                        @Param("keyword") String keyword,
                                        @Param("status") RegistrationStatus status,
                                        @Param("departmentId") Long departmentId,
                                        @Param("doctorId") Long doctorId,
                                        @Param("paid") Boolean paid,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
}
