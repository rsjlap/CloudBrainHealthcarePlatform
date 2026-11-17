package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.dto.response.registration.ScheduleVO;
import com.neusoft.cloudbrain.entity.Schedule;
import com.neusoft.cloudbrain.mapper.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleMapper scheduleMapper;

    @Transactional(readOnly = true)
    public List<ScheduleVO> listByDoctorInRange(Long doctorId, LocalDate startDate, LocalDate endDate) {
        return scheduleMapper.selectList(
                Wrappers.<Schedule>lambdaQuery()
                        .eq(Schedule::getDoctorId, doctorId)
                        .ge(Schedule::getWorkDate, startDate)
                        .le(Schedule::getWorkDate, endDate)
                        .orderByAsc(Schedule::getWorkDate)
        ).stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleVO> listByDoctor(Long doctorId, LocalDate date) {
        return scheduleMapper.selectList(
                Wrappers.<Schedule>lambdaQuery()
                        .eq(Schedule::getDoctorId, doctorId)
                        .eq(Schedule::getWorkDate, date)
        ).stream().map(this::toVO).toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleVO> getAvailableSlots(Long doctorId, LocalDate date) {
        return scheduleMapper.findAvailableSlots(doctorId, date)
                .stream().map(this::toVO).toList();
    }

    private ScheduleVO toVO(Schedule schedule) {
        int remaining = schedule.getMaxPatients() - schedule.getRegisteredCount();
        return ScheduleVO.builder()
                .id(schedule.getId())
                .doctorId(schedule.getDoctorId())
                .workDate(schedule.getWorkDate())
                .timeSlot(schedule.getTimeSlot())
                .maxPatients(schedule.getMaxPatients())
                .registeredCount(schedule.getRegisteredCount())
                .remainingSlots(Math.max(remaining, 0))
                .isAiGenerated(schedule.getIsAiGenerated())
                .build();
    }
}
