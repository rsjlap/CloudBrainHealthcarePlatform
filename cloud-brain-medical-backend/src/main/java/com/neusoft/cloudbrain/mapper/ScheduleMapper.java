package com.neusoft.cloudbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.neusoft.cloudbrain.common.enums.TimeSlot;
import com.neusoft.cloudbrain.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    List<Schedule> findAvailableSlots(@Param("doctorId") Long doctorId,
                                      @Param("workDate") LocalDate workDate);

    List<Schedule> search(@Param("deptId") Long deptId,
                          @Param("workDate") LocalDate workDate,
                          @Param("timeSlot") TimeSlot timeSlot);

    IPage<Schedule> searchPage(IPage<?> page,
                               @Param("deptId") Long deptId,
                               @Param("workDate") LocalDate workDate,
                               @Param("timeSlot") TimeSlot timeSlot);
}
