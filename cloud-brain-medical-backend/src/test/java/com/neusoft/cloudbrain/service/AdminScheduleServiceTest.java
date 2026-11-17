package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.TimeSlot;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.request.schedule.CreateScheduleRequest;
import com.neusoft.cloudbrain.dto.request.schedule.UpdateScheduleRequest;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.Doctor;
import com.neusoft.cloudbrain.entity.Schedule;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.DoctorMapper;
import com.neusoft.cloudbrain.mapper.ScheduleMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminScheduleService 排班管理")
class AdminScheduleServiceTest {

    @Mock
    private ScheduleMapper scheduleMapper;
    @Mock
    private DoctorMapper doctorMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DepartmentMapper departmentMapper;

    private AdminScheduleService adminScheduleService;

    @BeforeEach
    void setUp() {
        adminScheduleService = new AdminScheduleService(
                scheduleMapper, doctorMapper, userMapper, departmentMapper);
    }

    @Test
    @DisplayName("正常：创建排�?)
    void create_success() {
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setDepartmentId(10L);
        when(doctorMapper.selectById(1L)).thenReturn(doctor);
        when(scheduleMapper.selectCount(any())).thenReturn(0L);
        Schedule schedule = new Schedule();
        schedule.setId(5L);
        schedule.setDoctorId(1L);
        schedule.setWorkDate(LocalDate.of(2026, 6, 15));
        schedule.setTimeSlot(TimeSlot.MORNING);
        schedule.setMaxPatients(20);
        schedule.setRegisteredCount(0);
        when(scheduleMapper.selectById(5L)).thenReturn(schedule);
        User user = new User();
        user.setRealName("张医�?);
        when(userMapper.selectById(1L)).thenReturn(user);
        Department dept = new Department();
        dept.setName("内科");
        when(departmentMapper.selectById(10L)).thenReturn(dept);

        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setDoctorId(1L);
        request.setWorkDate(LocalDate.of(2026, 6, 15));
        request.setTimeSlot(TimeSlot.MORNING);
        request.setMaxPatients(20);

        var vo = adminScheduleService.create(request);

        verify(scheduleMapper).insert(any(Schedule.class));
        assertEquals(TimeSlot.MORNING, vo.getTimeSlot());
    }

    @Test
    @DisplayName("异常：医生不存在")
    void create_doctorNotFound() {
        when(doctorMapper.selectById(99L)).thenReturn(null);

        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setDoctorId(99L);
        request.setWorkDate(LocalDate.of(2026, 6, 15));
        request.setTimeSlot(TimeSlot.MORNING);
        request.setMaxPatients(20);

        BusinessException ex = assertThrows(BusinessException.class, () -> adminScheduleService.create(request));
        assertEquals(ResultCode.NOT_FOUND, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：号源上限小于已挂号人数")
    void update_maxPatientsTooSmall() {
        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setRegisteredCount(10);
        schedule.setMaxPatients(20);
        when(scheduleMapper.selectById(1L)).thenReturn(schedule);

        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setMaxPatients(5);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminScheduleService.update(1L, request));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：已有挂号时不可删除")
    void delete_withRegistrations() {
        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setRegisteredCount(3);
        when(scheduleMapper.selectById(1L)).thenReturn(schedule);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminScheduleService.delete(1L));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }
}
