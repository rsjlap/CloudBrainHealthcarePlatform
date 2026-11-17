package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.enums.TimeSlot;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.request.schedule.BatchPublishScheduleRequest;
import com.neusoft.cloudbrain.dto.request.schedule.CreateScheduleRequest;
import com.neusoft.cloudbrain.dto.request.schedule.UpdateScheduleRequest;
import com.neusoft.cloudbrain.dto.response.schedule.AdminScheduleVO;
import com.neusoft.cloudbrain.dto.response.schedule.BatchPublishScheduleResultVO;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.Doctor;
import com.neusoft.cloudbrain.entity.Schedule;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.DoctorMapper;
import com.neusoft.cloudbrain.mapper.ScheduleMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.neusoft.cloudbrain.common.result.PageResult;
import com.neusoft.cloudbrain.common.util.PageUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;

    @Transactional(readOnly = true)
    public PageResult<AdminScheduleVO> search(Long departmentId, LocalDate workDate, TimeSlot timeSlot,
                                              Long page, Long size) {
        IPage<Schedule> result = scheduleMapper.searchPage(
                PageUtil.of(page, size), departmentId, workDate, timeSlot);
        return PageResult.of(result, result.getRecords().stream().map(this::toAdminVO).toList());
    }

    @Transactional(readOnly = true)
    public List<AdminScheduleVO> searchAll(Long departmentId, LocalDate workDate, TimeSlot timeSlot) {
        return scheduleMapper.search(departmentId, workDate, timeSlot).stream()
                .map(this::toAdminVO)
                .toList();
    }

    @Transactional
    public AdminScheduleVO create(CreateScheduleRequest request) {
        Doctor doctor = doctorMapper.selectById(request.getDoctorId());
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在");
        }
        assertNoDuplicate(doctor.getId(), request.getWorkDate(), request.getTimeSlot(), null);

        Schedule schedule = new Schedule();
        schedule.setDoctorId(doctor.getId());
        schedule.setWorkDate(request.getWorkDate());
        schedule.setTimeSlot(request.getTimeSlot());
        schedule.setMaxPatients(request.getMaxPatients());
        schedule.setRegisteredCount(0);
        schedule.setIsAiGenerated(0);
        scheduleMapper.insert(schedule);
        return toAdminVO(schedule);
    }

    @Transactional
    public AdminScheduleVO update(Long id, UpdateScheduleRequest request) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "排班记录不存在");
        }

        if (request.getMaxPatients() < schedule.getRegisteredCount()) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "号源上限不能小于已挂号人数（" + schedule.getRegisteredCount() + "）");
        }
        schedule.setMaxPatients(request.getMaxPatients());

        if (schedule.getRegisteredCount() == 0) {
            Long doctorId = request.getDoctorId() != null ? request.getDoctorId() : schedule.getDoctorId();
            LocalDate workDate = request.getWorkDate() != null ? request.getWorkDate() : schedule.getWorkDate();
            TimeSlot timeSlot = request.getTimeSlot() != null ? request.getTimeSlot() : schedule.getTimeSlot();

            Doctor doctor = doctorMapper.selectById(doctorId);
            if (doctor == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "医生不存在");
            }
            assertNoDuplicate(doctorId, workDate, timeSlot, id);

            schedule.setDoctorId(doctorId);
            schedule.setWorkDate(workDate);
            schedule.setTimeSlot(timeSlot);
        } else if (request.getDoctorId() != null || request.getWorkDate() != null || request.getTimeSlot() != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "已有患者挂号，不可修改医生、日期或班次");
        }

        scheduleMapper.updateById(schedule);
        return toAdminVO(schedule);
    }

    @Transactional
    public void delete(Long id) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "排班记录不存在");
        }
        if (schedule.getRegisteredCount() > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "已有患者挂号，不可删除");
        }
        scheduleMapper.deleteById(id);
    }

    @Transactional
    public BatchPublishScheduleResultVO batchPublish(BatchPublishScheduleRequest request) {
        int successCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();
        for (CreateScheduleRequest item : request.getItems()) {
            try {
                Schedule existing = scheduleMapper.selectOne(
                        Wrappers.<Schedule>lambdaQuery()
                                .eq(Schedule::getDoctorId, item.getDoctorId())
                                .eq(Schedule::getWorkDate, item.getWorkDate())
                                .eq(Schedule::getTimeSlot, item.getTimeSlot())
                );
                if (existing != null) {
                    skippedCount++;
                    continue;
                }
                Doctor doctor = doctorMapper.selectById(item.getDoctorId());
                if (doctor == null) {
                    errors.add("医生不存在: " + item.getDoctorId());
                    continue;
                }
                Schedule schedule = new Schedule();
                schedule.setDoctorId(item.getDoctorId());
                schedule.setWorkDate(item.getWorkDate());
                schedule.setTimeSlot(item.getTimeSlot());
                schedule.setMaxPatients(item.getMaxPatients());
                schedule.setRegisteredCount(0);
                schedule.setIsAiGenerated(1);
                scheduleMapper.insert(schedule);
                successCount++;
            } catch (BusinessException ex) {
                errors.add(ex.getMessage());
            } catch (Exception ex) {
                errors.add("发布失败: " + ex.getMessage());
            }
        }
        return BatchPublishScheduleResultVO.builder()
                .successCount(successCount)
                .skippedCount(skippedCount)
                .errors(errors)
                .build();
    }

    private void assertNoDuplicate(Long doctorId, LocalDate workDate, TimeSlot timeSlot, Long excludeId) {
        Schedule existing = scheduleMapper.selectOne(
                Wrappers.<Schedule>lambdaQuery()
                        .eq(Schedule::getDoctorId, doctorId)
                        .eq(Schedule::getWorkDate, workDate)
                        .eq(Schedule::getTimeSlot, timeSlot)
        );
        if (existing != null && (excludeId == null || !existing.getId().equals(excludeId))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该医生在此日期班次已有排班");
        }
    }

    private AdminScheduleVO toAdminVO(Schedule schedule) {
        Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
        User user = doctor != null ? userMapper.selectById(doctor.getId()) : null;
        Department department = doctor != null && doctor.getDepartmentId() != null
                ? departmentMapper.selectById(doctor.getDepartmentId()) : null;
        int remaining = Math.max(schedule.getMaxPatients() - schedule.getRegisteredCount(), 0);
        LocalDate today = LocalDate.now();

        String slotStatus = remaining > 0 ? "有余号" : "已满";
        String dutyStatus;
        if (schedule.getWorkDate().isBefore(today)) {
            dutyStatus = "已结束";
        } else if (schedule.getWorkDate().isAfter(today)) {
            dutyStatus = "待出诊";
        } else {
            dutyStatus = "今日出诊";
        }

        return AdminScheduleVO.builder()
                .id(schedule.getId())
                .doctorId(doctor != null ? doctor.getId() : null)
                .doctorName(user != null ? user.getRealName() : null)
                .doctorTitle(doctor != null ? doctor.getTitle() : null)
                .departmentId(department != null ? department.getId() : null)
                .departmentName(department != null ? department.getName() : null)
                .doctorAvailable(doctor != null && doctor.getIsAvailable() != null && doctor.getIsAvailable() == 1)
                .workDate(schedule.getWorkDate())
                .timeSlot(schedule.getTimeSlot())
                .maxPatients(schedule.getMaxPatients())
                .registeredCount(schedule.getRegisteredCount())
                .remainingSlots(remaining)
                .slotStatus(slotStatus)
                .dutyStatus(dutyStatus)
                .isAiGenerated(schedule.getIsAiGenerated())
                .build();
    }
}
