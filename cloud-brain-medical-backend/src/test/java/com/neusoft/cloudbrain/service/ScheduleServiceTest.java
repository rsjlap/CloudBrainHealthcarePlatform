package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.TimeSlot;
import com.neusoft.cloudbrain.dto.response.registration.ScheduleVO;
import com.neusoft.cloudbrain.entity.Schedule;
import com.neusoft.cloudbrain.mapper.ScheduleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService ????")
class ScheduleServiceTest {

    @Mock
    private ScheduleMapper scheduleMapper;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(scheduleMapper);
    }

    @Test
    @DisplayName("??????????????)
    void listByDoctorInRange() {
        Schedule schedule = buildSchedule(10, 8);
        when(scheduleMapper.selectList(any())).thenReturn(List.of(schedule));

        List<ScheduleVO> result = scheduleService.listByDoctorInRange(1L,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getRemainingSlots());
    }

    @Test
    @DisplayName("????????????)
    void remainingSlots_notNegative() {
        Schedule schedule = buildSchedule(5, 10);
        when(scheduleMapper.selectList(any())).thenReturn(List.of(schedule));

        List<ScheduleVO> result = scheduleService.listByDoctor(1L, LocalDate.of(2026, 6, 15));

        assertEquals(0, result.get(0).getRemainingSlots());
    }

    @Test
    @DisplayName("??????????")
    void getAvailableSlots() {
        Schedule schedule = buildSchedule(20, 5);
        when(scheduleMapper.findAvailableSlots(eq(1L), eq(LocalDate.of(2026, 6, 15))))
                .thenReturn(List.of(schedule));

        List<ScheduleVO> result = scheduleService.getAvailableSlots(1L, LocalDate.of(2026, 6, 15));

        assertEquals(1, result.size());
        assertEquals(15, result.get(0).getRemainingSlots());
    }

    private static Schedule buildSchedule(int maxPatients, int registeredCount) {
        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setDoctorId(1L);
        schedule.setWorkDate(LocalDate.of(2026, 6, 15));
        schedule.setTimeSlot(TimeSlot.MORNING);
        schedule.setMaxPatients(maxPatients);
        schedule.setRegisteredCount(registeredCount);
        schedule.setIsAiGenerated(0);
        return schedule;
    }
}
