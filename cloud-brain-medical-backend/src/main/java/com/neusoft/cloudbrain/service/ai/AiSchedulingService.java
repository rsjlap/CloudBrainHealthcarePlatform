package com.neusoft.cloudbrain.service.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.constant.AiPromptConstants;
import com.neusoft.cloudbrain.common.enums.TimeSlot;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.response.ai.AiScheduleGenerateResultVO;
import com.neusoft.cloudbrain.dto.response.ai.AiScheduleItemDraft;
import com.neusoft.cloudbrain.dto.response.ai.ScheduleVO;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.Doctor;
import com.neusoft.cloudbrain.entity.Schedule;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.DoctorMapper;
import com.neusoft.cloudbrain.mapper.ScheduleMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSchedulingService {

    private static final int DEFAULT_MAX_PATIENTS = 20;

    private final DoctorMapper doctorMapper;
    private final ScheduleMapper scheduleMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final AiPromptService aiPromptService;
    private final AiLlmClient aiLlmClient;

    @Transactional(readOnly = true)
    public AiScheduleGenerateResultVO generateSchedule(Long deptId, LocalDate startDate, LocalDate endDate) {
        validateParams(deptId, startDate, endDate);
        Department department = requireDepartment(deptId);
        List<Doctor> doctors = loadAvailableDoctors(deptId);
        List<Schedule> existing = loadExistingSchedules(deptId, startDate, endDate);

        if (!aiLlmClient.isAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR,
                    "DeepSeek AI 未启用，请检查 app.ai.enabled 与 API Key 配置");
        }

        try {
            String prompt = aiPromptService.render(AiPromptConstants.SCHEDULING, Map.of(
                    "department", department.getName(),
                    "doctors", buildDoctorsText(doctors),
                    "existingSchedules", buildExistingSchedulesText(existing, doctors),
                    "startDate", startDate.toString(),
                    "endDate", endDate.toString()
            ));
            List<AiScheduleItemDraft> drafts = aiLlmClient.chatForJsonList(prompt, AiScheduleItemDraft.class);
            List<ScheduleVO> schedules = normalizeDrafts(drafts, doctors, existing, startDate, endDate);
            schedules = fillMissingShiftCoverage(schedules, doctors, existing, startDate, endDate);
            if (schedules.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "DeepSeek 未返回有效排班建议，请调整日期范围后重试");
            }
            log.info("DeepSeek scheduling generated {} suggestions for deptId={} {}~{}",
                    schedules.size(), deptId, startDate, endDate);
            return AiScheduleGenerateResultVO.builder()
                    .schedules(schedules)
                    .aiAvailable(true)
                    .summary("已由 DeepSeek 生成 " + schedules.size() + " 条排班建议，可在发布前手动调整")
                    .build();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("DeepSeek scheduling failed deptId={} {}~{}", deptId, startDate, endDate, ex);
            throw new BusinessException(ResultCode.SERVER_ERROR,
                    "DeepSeek 排班生成失败：" + ex.getMessage());
        }
    }

    private void validateParams(Long deptId, LocalDate startDate, LocalDate endDate) {
        if (deptId == null || startDate == null || endDate == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "科室与日期范围不能为空");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
    }

    private Department requireDepartment(Long deptId) {
        Department department = departmentMapper.selectById(deptId);
        if (department == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "科室不存在");
        }
        return department;
    }

    private List<Doctor> loadAvailableDoctors(Long deptId) {
        List<Doctor> doctors = doctorMapper.selectList(
                Wrappers.<Doctor>lambdaQuery()
                        .eq(Doctor::getDepartmentId, deptId)
                        .eq(Doctor::getIsAvailable, 1)
                        .orderByAsc(Doctor::getId)
        );
        if (doctors.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该科室暂无可用医生，无法生成排班");
        }
        return doctors;
    }

    private List<Schedule> loadExistingSchedules(Long deptId, LocalDate startDate, LocalDate endDate) {
        List<Doctor> deptDoctors = doctorMapper.selectList(
                Wrappers.<Doctor>lambdaQuery().eq(Doctor::getDepartmentId, deptId)
        );
        if (deptDoctors.isEmpty()) {
            return List.of();
        }
        List<Long> doctorIds = deptDoctors.stream().map(Doctor::getId).toList();
        return scheduleMapper.selectList(
                Wrappers.<Schedule>lambdaQuery()
                        .in(Schedule::getDoctorId, doctorIds)
                        .ge(Schedule::getWorkDate, startDate)
                        .le(Schedule::getWorkDate, endDate)
        );
    }

    private String buildDoctorsText(List<Doctor> doctors) {
        StringBuilder sb = new StringBuilder();
        for (Doctor doctor : doctors) {
            User user = userMapper.selectById(doctor.getId());
            sb.append(String.format("- id=%d name=%s title=%s specialty=%s%n",
                    doctor.getId(),
                    user != null ? user.getRealName() : "未知",
                    defaultText(doctor.getTitle()),
                    defaultText(doctor.getSpecialty())));
        }
        return sb.toString();
    }

    private String buildExistingSchedulesText(List<Schedule> existing, List<Doctor> doctors) {
        if (existing.isEmpty()) {
            return "（无）";
        }
        Map<Long, String> nameMap = doctors.stream()
                .collect(Collectors.toMap(Doctor::getId, d -> {
                    User user = userMapper.selectById(d.getId());
                    return user != null ? user.getRealName() : "医生";
                }, (a, b) -> a));
        StringBuilder sb = new StringBuilder();
        for (Schedule schedule : existing) {
            sb.append(String.format("- doctorId=%d name=%s workDate=%s timeSlot=%s%n",
                    schedule.getDoctorId(),
                    nameMap.getOrDefault(schedule.getDoctorId(), "医生"),
                    schedule.getWorkDate(),
                    schedule.getTimeSlot()));
        }
        return sb.toString();
    }

    private List<ScheduleVO> normalizeDrafts(List<AiScheduleItemDraft> drafts,
                                           List<Doctor> doctors,
                                           List<Schedule> existing,
                                           LocalDate startDate,
                                           LocalDate endDate) {
        Set<Long> doctorIds = doctors.stream().map(Doctor::getId).collect(Collectors.toSet());
        Set<String> occupied = new HashSet<>();
        for (Schedule schedule : existing) {
            occupied.add(slotKey(schedule.getDoctorId(), schedule.getWorkDate(), schedule.getTimeSlot()));
        }

        List<ScheduleVO> result = new ArrayList<>();
        for (AiScheduleItemDraft draft : drafts != null ? drafts : List.<AiScheduleItemDraft>of()) {
            if (draft.getDoctorId() == null || !doctorIds.contains(draft.getDoctorId())) {
                continue;
            }
            if (draft.getWorkDate() == null
                    || draft.getWorkDate().isBefore(startDate)
                    || draft.getWorkDate().isAfter(endDate)) {
                continue;
            }
            if (draft.getTimeSlot() == null) {
                continue;
            }
            String key = slotKey(draft.getDoctorId(), draft.getWorkDate(), draft.getTimeSlot());
            if (occupied.contains(key)) {
                continue;
            }
            occupied.add(key);

            User user = userMapper.selectById(draft.getDoctorId());
            int maxPatients = draft.getMaxPatients() != null && draft.getMaxPatients() > 0
                    ? draft.getMaxPatients() : DEFAULT_MAX_PATIENTS;
            result.add(ScheduleVO.builder()
                    .doctorId(draft.getDoctorId())
                    .doctorName(StringUtils.hasText(draft.getDoctorName())
                            ? draft.getDoctorName()
                            : (user != null ? user.getRealName() : "医生"))
                    .workDate(draft.getWorkDate())
                    .timeSlot(draft.getTimeSlot())
                    .maxPatients(maxPatients)
                    .reason(StringUtils.hasText(draft.getReason()) ? draft.getReason() : "DeepSeek 推荐")
                    .build());
        }
        return result;
    }

    /**
     * DeepSeek 有时只排上午/下午，对工作日缺失的班次（尤其 EVENING）做兜底补全。
     */
    private List<ScheduleVO> fillMissingShiftCoverage(List<ScheduleVO> schedules,
                                                      List<Doctor> doctors,
                                                      List<Schedule> existing,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        Set<Long> doctorIds = doctors.stream().map(Doctor::getId).collect(Collectors.toSet());
        Set<String> slotOccupied = new HashSet<>();
        Set<String> doctorDayOccupied = new HashSet<>();

        for (Schedule schedule : existing) {
            if (!doctorIds.contains(schedule.getDoctorId())) {
                continue;
            }
            slotOccupied.add(slotKey(schedule.getDoctorId(), schedule.getWorkDate(), schedule.getTimeSlot()));
            doctorDayOccupied.add(doctorDayKey(schedule.getDoctorId(), schedule.getWorkDate()));
        }

        List<ScheduleVO> result = new ArrayList<>(schedules);
        for (ScheduleVO schedule : schedules) {
            slotOccupied.add(slotKey(schedule.getDoctorId(), schedule.getWorkDate(), schedule.getTimeSlot()));
            doctorDayOccupied.add(doctorDayKey(schedule.getDoctorId(), schedule.getWorkDate()));
        }

        int doctorIndex = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (isWeekend(date)) {
                continue;
            }
            for (TimeSlot slot : TimeSlot.values()) {
                if (hasShiftCoverage(result, existing, doctorIds, date, slot)) {
                    continue;
                }
                Doctor doctor = pickDoctorForSlot(doctors, date, slot, slotOccupied, doctorDayOccupied, doctorIndex);
                if (doctor == null) {
                    continue;
                }
                doctorIndex++;
                User user = userMapper.selectById(doctor.getId());
                ScheduleVO filled = ScheduleVO.builder()
                        .doctorId(doctor.getId())
                        .doctorName(user != null ? user.getRealName() : "医生")
                        .workDate(date)
                        .timeSlot(slot)
                        .maxPatients(DEFAULT_MAX_PATIENTS)
                        .reason(slotLabel(slot) + "补班（AI 未覆盖时自动补全）")
                        .build();
                result.add(filled);
                slotOccupied.add(slotKey(doctor.getId(), date, slot));
                doctorDayOccupied.add(doctorDayKey(doctor.getId(), date));
            }
        }
        return result;
    }

    private boolean hasShiftCoverage(List<ScheduleVO> schedules,
                                     List<Schedule> existing,
                                     Set<Long> doctorIds,
                                     LocalDate date,
                                     TimeSlot slot) {
        for (ScheduleVO schedule : schedules) {
            if (schedule.getWorkDate().equals(date) && schedule.getTimeSlot() == slot) {
                return true;
            }
        }
        for (Schedule schedule : existing) {
            if (doctorIds.contains(schedule.getDoctorId())
                    && schedule.getWorkDate().equals(date)
                    && schedule.getTimeSlot() == slot) {
                return true;
            }
        }
        return false;
    }

    private Doctor pickDoctorForSlot(List<Doctor> doctors,
                                     LocalDate date,
                                     TimeSlot slot,
                                     Set<String> slotOccupied,
                                     Set<String> doctorDayOccupied,
                                     int startIndex) {
        for (int i = 0; i < doctors.size(); i++) {
            Doctor doctor = doctors.get((startIndex + i) % doctors.size());
            if (doctorDayOccupied.contains(doctorDayKey(doctor.getId(), date))) {
                continue;
            }
            if (!slotOccupied.contains(slotKey(doctor.getId(), date, slot))) {
                return doctor;
            }
        }
        return null;
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private static String doctorDayKey(Long doctorId, LocalDate date) {
        return doctorId + "|" + date;
    }

    private static String slotLabel(TimeSlot slot) {
        return switch (slot) {
            case MORNING -> "上午";
            case AFTERNOON -> "下午";
            case EVENING -> "晚上";
        };
    }

    private static String slotKey(Long doctorId, LocalDate date, TimeSlot slot) {
        return doctorId + "|" + date + "|" + slot.name();
    }

    private static String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
