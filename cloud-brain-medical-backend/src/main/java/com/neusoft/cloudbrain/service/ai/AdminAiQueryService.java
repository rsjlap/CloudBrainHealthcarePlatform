package com.neusoft.cloudbrain.service.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.constant.AiPromptConstants;
import com.neusoft.cloudbrain.common.enums.EquipmentStatus;
import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import com.neusoft.cloudbrain.common.enums.TimeSlot;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.dto.response.admin.AdminAiChatResponseVO;
import com.neusoft.cloudbrain.dto.response.admin.AdminAiQueryIntentDraft;
import com.neusoft.cloudbrain.dto.response.admin.AdminDashboardVO;
import com.neusoft.cloudbrain.dto.response.equipment.EquipmentStatsVO;
import com.neusoft.cloudbrain.dto.response.patient.AdminPatientStatsVO;
import com.neusoft.cloudbrain.dto.response.registration.RegistrationVO;
import com.neusoft.cloudbrain.dto.response.schedule.AdminScheduleVO;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.Drug;
import com.neusoft.cloudbrain.entity.MedicalEquipment;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.DrugMapper;
import com.neusoft.cloudbrain.mapper.MedicalEquipmentMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.service.AdminEquipmentService;
import com.neusoft.cloudbrain.service.AdminPatientService;
import com.neusoft.cloudbrain.service.AdminRegistrationService;
import com.neusoft.cloudbrain.service.AdminScheduleService;
import com.neusoft.cloudbrain.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAiQueryService {

    private static final int DEFAULT_STOCK_THRESHOLD = 10;
    private static final int MAX_DRUG_ROWS = 30;
    private static final int MAX_LIST_ROWS = 20;
    private static final Pattern DEPT_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,10}科");

    private final AiLlmClient aiLlmClient;
    private final AiPromptService aiPromptService;
    private final AdminService adminService;
    private final AdminRegistrationService adminRegistrationService;
    private final AdminEquipmentService adminEquipmentService;
    private final AdminPatientService adminPatientService;
    private final AdminScheduleService adminScheduleService;
    private final DrugMapper drugMapper;
    private final DepartmentMapper departmentMapper;
    private final MedicalEquipmentMapper equipmentMapper;
    private final RegistrationMapper registrationMapper;

    @Transactional(readOnly = true)
    public AdminAiChatResponseVO chat(String question) {
        if (!StringUtils.hasText(question)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "问题不能为空");
        }
        String normalizedQuestion = question.trim();
        if (!aiLlmClient.isAvailable()) {
            throw new BusinessException(ResultCode.SERVER_ERROR,
                    "DeepSeek AI 未启用，请检查 app.ai.enabled 与 API Key 配置");
        }

        AdminAiQueryIntentDraft intent = parseIntent(normalizedQuestion);
        enrichIntentFromQuestion(intent, normalizedQuestion);
        ContextBundle bundle = buildSmartContext(intent, normalizedQuestion);

        try {
            String answerPrompt = aiPromptService.render(AiPromptConstants.ADMIN_QUERY_ANSWER, Map.of(
                    "question", normalizedQuestion,
                    "context", bundle.context()
            ));
            String answer = aiLlmClient.chat(answerPrompt).trim();
            return AdminAiChatResponseVO.builder()
                    .answer(answer)
                    .queryType(bundle.queryType())
                    .sources(bundle.sources())
                    .aiAvailable(true)
                    .build();
        } catch (Exception ex) {
            log.error("Admin AI query failed: {}", normalizedQuestion, ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "AI 查询失败：" + ex.getMessage());
        }
    }

    private AdminAiQueryIntentDraft parseIntent(String question) {
        try {
            String prompt = aiPromptService.render(AiPromptConstants.ADMIN_QUERY_INTENT, Map.of(
                    "question", question
            ));
            AdminAiQueryIntentDraft draft = aiLlmClient.chatForJson(prompt, AdminAiQueryIntentDraft.class);
            if (!StringUtils.hasText(draft.getIntent())) {
                draft.setIntent(fallbackIntent(question));
            }
            return draft;
        } catch (Exception ex) {
            log.warn("Intent parse fallback for question: {}", question, ex);
            AdminAiQueryIntentDraft draft = new AdminAiQueryIntentDraft();
            draft.setIntent(fallbackIntent(question));
            draft.setKeyword(extractDrugKeyword(question));
            draft.setStockThreshold(DEFAULT_STOCK_THRESHOLD);
            return draft;
        }
    }

    private void enrichIntentFromQuestion(AdminAiQueryIntentDraft intent, String question) {
        if (isScheduleAvailabilityQuestion(question)) {
            intent.setIntent("SCHEDULE");
            if (intent.getOnlyAvailable() == null) {
                intent.setOnlyAvailable(true);
            }
        }
        if (!StringUtils.hasText(intent.getDepartmentName())) {
            intent.setDepartmentName(extractDepartmentName(question));
        }
        if (!StringUtils.hasText(intent.getKeyword())) {
            intent.setKeyword(extractDoctorKeyword(question, intent.getDepartmentName()));
        }
        if (!StringUtils.hasText(intent.getDateScope())) {
            intent.setDateScope(inferDateScope(question));
        }
        if (intent.getOnlyAvailable() == null && containsAny(question, "余号", "号源", "还能", "可不可以", "能不能")) {
            intent.setOnlyAvailable(true);
        }
    }

    private ContextBundle buildSmartContext(AdminAiQueryIntentDraft intent, String question) {
        String queryType = normalizeIntent(intent.getIntent());
        StringBuilder sb = new StringBuilder();
        List<String> sources = new ArrayList<>();

        switch (queryType) {
            case "DRUG" -> appendDrugContext(sb, sources, intent, question);
            case "REGISTRATION" -> appendRegistrationContext(sb, sources, intent, question);
            case "EQUIPMENT" -> appendEquipmentContext(sb, sources, intent);
            case "PATIENT" -> appendPatientContext(sb, sources);
            case "SCHEDULE" -> appendScheduleContext(sb, sources, intent, question);
            case "STAFF" -> appendStaffContext(sb, sources);
            default -> appendDashboardContext(sb, sources);
        }

        if (isScheduleAvailabilityQuestion(question) && !"SCHEDULE".equals(queryType)) {
            sb.append("\n\n");
            appendScheduleContext(sb, sources, intent, question);
            queryType = "SCHEDULE";
        }

        return new ContextBundle(sb.toString().trim(), sources.stream().distinct().toList(), queryType);
    }

    private void appendDrugContext(StringBuilder sb, List<String> sources,
                                   AdminAiQueryIntentDraft intent, String question) {
        sources.add("drug 药品表");
        int threshold = intent.getStockThreshold() != null && intent.getStockThreshold() > 0
                ? intent.getStockThreshold() : DEFAULT_STOCK_THRESHOLD;
        String keyword = StringUtils.hasText(intent.getKeyword()) ? intent.getKeyword().trim() : extractDrugKeyword(question);

        if (StringUtils.hasText(keyword)) {
            List<Drug> matched = searchDrugs(keyword, MAX_DRUG_ROWS);
            sb.append("## 关键词「").append(keyword).append("」匹配的药品（共 ").append(matched.size()).append(" 条）\n");
            appendDrugTable(sb, matched);
        }

        if (containsAny(question, "低", "少", "预警", "不足", "缺")) {
            List<Drug> lowStock = drugMapper.selectList(
                    Wrappers.<Drug>lambdaQuery()
                            .eq(Drug::getStatus, 1)
                            .lt(Drug::getStockQuantity, threshold)
                            .orderByAsc(Drug::getStockQuantity)
                            .last("LIMIT " + MAX_DRUG_ROWS)
            );
            sb.append("\n## 库存低于 ").append(threshold).append(" 的在售药品（共 ")
                    .append(lowStock.size()).append(" 条）\n");
            appendDrugTable(sb, lowStock);
        }

        if (sb.isEmpty()) {
            List<Drug> all = drugMapper.selectList(
                    Wrappers.<Drug>lambdaQuery()
                            .orderByDesc(Drug::getStatus)
                            .orderByAsc(Drug::getStockQuantity)
                            .last("LIMIT " + MAX_DRUG_ROWS)
            );
            sb.append("## 药品库存概览（前 ").append(all.size()).append(" 条）\n");
            appendDrugTable(sb, all);
        }
    }

    private void appendRegistrationContext(StringBuilder sb, List<String> sources,
                                           AdminAiQueryIntentDraft intent, String question) {
        sources.add("registration 挂号表");
        AdminDashboardVO dashboard = adminService.getDashboardStats();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        sb.append("## 挂号统计\n");
        sb.append("- 今日挂号数：").append(dashboard.getTodayRegistrations()).append('\n');
        sb.append("- 累计挂号数：").append(dashboard.getTotalRegistrations()).append('\n');
        sb.append("- 就诊中：").append(dashboard.getInProgressCount()).append('\n');
        sb.append("- 待就诊：").append(registrationMapper.selectCount(
                Wrappers.<Registration>lambdaQuery().eq(Registration::getStatus, RegistrationStatus.PENDING)
        )).append('\n');

        if (StringUtils.hasText(intent.getKeyword()) || containsAny(question, "最近", "列表", "记录")) {
            LocalDate dateFrom = "TODAY".equals(intent.getDateScope()) ? LocalDate.now() : null;
            var page = adminRegistrationService.search(
                    intent.getKeyword(), null, null, null, null, dateFrom, LocalDate.now(),
                    1L, (long) MAX_LIST_ROWS);
            sb.append("\n## 挂号记录（").append(page.getRecords().size()).append(" 条）\n");
            for (RegistrationVO row : page.getRecords()) {
                sb.append("- ").append(row.getPatientName())
                        .append(" → ").append(row.getDeptName())
                        .append(' ').append(row.getDoctorName())
                        .append(" （").append(row.getStatus()).append("）\n");
            }
        }
    }

    private void appendEquipmentContext(StringBuilder sb, List<String> sources, AdminAiQueryIntentDraft intent) {
        sources.add("medical_equipment 设备表");
        EquipmentStatsVO stats = adminEquipmentService.getStats();
        sb.append("## 设备统计\n");
        sb.append("- 总数：").append(stats.getTotalCount())
                .append("，正常：").append(stats.getActiveCount())
                .append("，故障：").append(stats.getFaultCount()).append('\n');

        List<MedicalEquipment> faultList = equipmentMapper.search(null, EquipmentStatus.FAULT, intent.getKeyword());
        if (!faultList.isEmpty()) {
            sb.append("\n## 故障设备\n");
            faultList.stream().limit(MAX_LIST_ROWS).forEach(e ->
                    sb.append("- ").append(e.getEquipmentName())
                            .append("（").append(nullToDash(e.getLocation())).append("）\n"));
        }
    }

    private void appendPatientContext(StringBuilder sb, List<String> sources) {
        sources.add("patient 患者表");
        AdminPatientStatsVO stats = adminPatientService.getStats();
        sb.append("## 患者统计\n");
        sb.append("- 注册总数：").append(stats.getTotalCount()).append('\n');
        sb.append("- 有过敏史：").append(stats.getAllergyCount()).append('\n');
        sb.append("- 今日新增：").append(stats.getTodayNewCount()).append('\n');
    }

    private void appendScheduleContext(StringBuilder sb, List<String> sources,
                                       AdminAiQueryIntentDraft intent, String question) {
        sources.add("schedule 排班表");
        sources.add("department 科室表");

        Department department = resolveDepartment(intent, question);
        DateRange range = resolveDateRange(intent, question);
        boolean onlyAvailable = Boolean.TRUE.equals(intent.getOnlyAvailable())
                || isScheduleAvailabilityQuestion(question);

        List<AdminScheduleVO> schedules = loadSchedules(department, range);

        List<AdminScheduleVO> filtered = schedules.stream()
                .filter(s -> !s.getWorkDate().isBefore(range.start()) && !s.getWorkDate().isAfter(range.end()))
                .filter(s -> matchesDoctorKeyword(s, intent.getKeyword(), intent.getDepartmentName()))
                .sorted(Comparator.comparing(AdminScheduleVO::getWorkDate).thenComparing(AdminScheduleVO::getTimeSlot))
                .toList();

        List<AdminScheduleVO> bookable = filtered.stream()
                .filter(this::isBookable)
                .toList();

        List<AdminScheduleVO> display = onlyAvailable ? bookable : filtered;

        sb.append("## 排班查询条件\n");
        sb.append("- 科室：").append(department != null ? department.getName() : "全部科室").append('\n');
        sb.append("- 日期范围：").append(range.label()).append('\n');
        sb.append("- 仅看可挂号：").append(onlyAvailable ? "是" : "否").append('\n');
        sb.append("\n## 查询结论\n");
        if (bookable.isEmpty()) {
            sb.append("- 结论：").append(range.label())
                    .append(department != null ? department.getName() : "所选范围")
                    .append(" **暂无可供患者挂号的医生**（无余号或医生停诊）\n");
            if (!filtered.isEmpty()) {
                sb.append("- 说明：共有 ").append(filtered.size())
                        .append(" 条排班，但均已满号或医生不可接诊\n");
            } else {
                sb.append("- 说明：该条件下 **没有任何排班记录**\n");
            }
        } else {
            sb.append("- 结论：**可以挂号**，共有 ").append(bookable.size()).append(" 个可预约班次\n");
            long doctorCount = bookable.stream().map(AdminScheduleVO::getDoctorId).distinct().count();
            sb.append("- 可接诊医生数：").append(doctorCount).append('\n');
        }

        sb.append("\n## 排班明细（").append(display.size()).append(" 条）\n");
        if (display.isEmpty()) {
            sb.append("（无匹配记录）\n");
        } else {
            for (AdminScheduleVO row : display.stream().limit(MAX_LIST_ROWS).toList()) {
                boolean bookableFlag = isBookable(row);
                sb.append("- 【").append(bookableFlag ? "可挂号" : "不可挂号").append("】")
                        .append(row.getWorkDate()).append(' ')
                        .append(timeSlotLabel(row.getTimeSlot())).append(' ')
                        .append(nullToDash(row.getDepartmentName())).append(' ')
                        .append(row.getDoctorName())
                        .append("（").append(nullToDash(row.getDoctorTitle())).append("）")
                        .append(" 余号=").append(safeRemaining(row))
                        .append('/').append(row.getMaxPatients())
                        .append(" 医生状态=").append(Boolean.TRUE.equals(row.getDoctorAvailable()) ? "可接诊" : "停诊")
                        .append('\n');
            }
        }

        if (department == null && StringUtils.hasText(intent.getDepartmentName())) {
            sb.append("\n（未在系统中匹配到科室「").append(intent.getDepartmentName())
                    .append("」，以上为全部科室数据）\n");
            appendDepartmentHint(sb);
        }

        if (bookable.isEmpty() && onlyAvailable && range.start().equals(LocalDate.now())) {
            appendUpcomingBookableHint(sb, department, intent.getKeyword());
        }
    }

    private List<AdminScheduleVO> loadSchedules(Department department, DateRange range) {
        Long deptId = department != null ? department.getId() : null;
        if (range.start().equals(range.end())) {
            return adminScheduleService.searchAll(deptId, range.start(), null);
        }
        return adminScheduleService.searchAll(deptId, null, null);
    }

    private boolean matchesDoctorKeyword(AdminScheduleVO schedule, String keyword, String departmentName) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String kw = keyword.trim();
        if (StringUtils.hasText(departmentName) && (kw.equals(departmentName) || kw.contains(departmentName))) {
            return true;
        }
        if (kw.endsWith("科")) {
            return true;
        }
        return schedule.getDoctorName() != null && schedule.getDoctorName().contains(kw);
    }

    private void appendUpcomingBookableHint(StringBuilder sb, Department department, String doctorKeyword) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(7);
        List<AdminScheduleVO> upcoming = adminScheduleService.searchAll(
                        department != null ? department.getId() : null, null, null)
                .stream()
                .filter(s -> s.getWorkDate().isAfter(today) && !s.getWorkDate().isAfter(end))
                .filter(this::isBookable)
                .filter(s -> matchesDoctorKeyword(s, doctorKeyword, null))
                .sorted(Comparator.comparing(AdminScheduleVO::getWorkDate).thenComparing(AdminScheduleVO::getTimeSlot))
                .limit(5)
                .toList();
        if (upcoming.isEmpty()) {
            return;
        }
        sb.append("\n## 近期可挂号推荐（今天无号，未来7天内）\n");
        for (AdminScheduleVO row : upcoming) {
            sb.append("- ").append(row.getWorkDate()).append(' ')
                    .append(timeSlotLabel(row.getTimeSlot())).append(' ')
                    .append(nullToDash(row.getDepartmentName())).append(' ')
                    .append(row.getDoctorName())
                    .append(" 余号=").append(safeRemaining(row)).append('\n');
        }
    }

    private void appendStaffContext(StringBuilder sb, List<String> sources) {
        sources.add("user 用户表");
        AdminDashboardVO dashboard = adminService.getDashboardStats();
        sb.append("## 工作人员\n");
        sb.append("- 在职医生：").append(dashboard.getDoctorCount()).append('\n');
    }

    private void appendDashboardContext(StringBuilder sb, List<String> sources) {
        sources.add("仪表盘统计");
        AdminDashboardVO stats = adminService.getDashboardStats();
        sb.append("## 业务概览\n");
        sb.append("- 今日挂号：").append(stats.getTodayRegistrations()).append('\n');
        sb.append("- 注册患者：").append(stats.getPatientCount()).append('\n');
        sb.append("- 在职医生：").append(stats.getDoctorCount()).append('\n');
    }

    private void appendDepartmentHint(StringBuilder sb) {
        List<Department> departments = departmentMapper.selectList(
                Wrappers.<Department>lambdaQuery().orderByAsc(Department::getSortOrder));
        if (!departments.isEmpty()) {
            sb.append("\n## 系统现有科室\n");
            departments.forEach(d -> sb.append("- ").append(d.getName()).append('\n'));
        }
    }

    private Department resolveDepartment(AdminAiQueryIntentDraft intent, String question) {
        String deptName = StringUtils.hasText(intent.getDepartmentName())
                ? intent.getDepartmentName().trim() : extractDepartmentName(question);
        if (!StringUtils.hasText(deptName)) {
            return null;
        }
        List<Department> departments = departmentMapper.selectList(null);
        Department exact = departments.stream()
                .filter(d -> deptName.equals(d.getName()))
                .findFirst().orElse(null);
        if (exact != null) {
            return exact;
        }
        return departments.stream()
                .filter(d -> d.getName().contains(deptName) || deptName.contains(d.getName()))
                .max(Comparator.comparingInt(d -> overlapScore(deptName, d.getName())))
                .orElse(null);
    }

    private int overlapScore(String a, String b) {
        if (a.contains(b) || b.contains(a)) {
            return Math.min(a.length(), b.length());
        }
        return 0;
    }

    private DateRange resolveDateRange(AdminAiQueryIntentDraft intent, String question) {
        String scope = StringUtils.hasText(intent.getDateScope())
                ? intent.getDateScope().trim().toUpperCase(Locale.ROOT) : inferDateScope(question);
        LocalDate today = LocalDate.now();
        return switch (scope) {
            case "TOMORROW" -> new DateRange(today.plusDays(1), today.plusDays(1), "明天（" + today.plusDays(1) + "）");
            case "WEEK" -> new DateRange(today, today.plusDays(6), "本周（" + today + " 至 " + today.plusDays(6) + "）");
            case "ALL" -> new DateRange(today.minusDays(7), today.plusDays(30), "近远期");
            default -> new DateRange(today, today, "今天（" + today + "）");
        };
    }

    private boolean isBookable(AdminScheduleVO schedule) {
        if (!Boolean.TRUE.equals(schedule.getDoctorAvailable())) {
            return false;
        }
        return safeRemaining(schedule) > 0;
    }

    private int safeRemaining(AdminScheduleVO schedule) {
        if (schedule.getRemainingSlots() != null) {
            return schedule.getRemainingSlots();
        }
        int max = schedule.getMaxPatients() != null ? schedule.getMaxPatients() : 0;
        int registered = schedule.getRegisteredCount() != null ? schedule.getRegisteredCount() : 0;
        return Math.max(max - registered, 0);
    }

    private String timeSlotLabel(TimeSlot slot) {
        if (slot == null) {
            return "-";
        }
        return switch (slot) {
            case MORNING -> "上午";
            case AFTERNOON -> "下午";
            case EVENING -> "晚上";
        };
    }

    private boolean isScheduleAvailabilityQuestion(String question) {
        boolean aboutBooking = containsAny(question,
                "挂号", "预约", "余号", "号源", "还能", "还有没有", "有没有", "可不可以", "能不能", "可挂");
        boolean aboutSchedule = containsAny(question,
                "医生", "出诊", "排班", "科", "班次", "接诊");
        return (aboutBooking && aboutSchedule)
                || containsAny(question, "排班", "出诊", "余号", "号源");
    }

    private String inferDateScope(String question) {
        if (containsAny(question, "明天", "明日")) {
            return "TOMORROW";
        }
        if (containsAny(question, "本周", "这周", "一周")) {
            return "WEEK";
        }
        if (containsAny(question, "今天", "今日", "当天")) {
            return "TODAY";
        }
        return isScheduleAvailabilityQuestion(question) ? "TODAY" : "ALL";
    }

    private String extractDepartmentName(String question) {
        List<Department> departments = departmentMapper.selectList(null);
        Department bestPartial = null;
        int bestScore = 0;
        for (Department dept : departments) {
            if (question.contains(dept.getName())) {
                return dept.getName();
            }
            int score = departmentMatchScore(dept.getName(), question);
            if (score > bestScore) {
                bestScore = score;
                bestPartial = dept;
            }
        }
        if (bestPartial != null && bestScore >= 2) {
            return bestPartial.getName();
        }
        Matcher matcher = DEPT_PATTERN.matcher(question);
        if (matcher.find()) {
            String candidate = matcher.group();
            for (Department dept : departments) {
                if (dept.getName().contains(candidate) || candidate.contains(dept.getName())) {
                    return dept.getName();
                }
            }
            return candidate;
        }
        return "";
    }

    private int departmentMatchScore(String deptName, String question) {
        int score = 0;
        for (int len = Math.min(deptName.length(), 4); len >= 2; len--) {
            for (int i = 0; i <= deptName.length() - len; i++) {
                String part = deptName.substring(i, i + len);
                if (question.contains(part)) {
                    score = Math.max(score, len);
                }
            }
        }
        return score;
    }

    private String extractDoctorKeyword(String question, String departmentName) {
        String q = question;
        if (StringUtils.hasText(departmentName)) {
            q = q.replace(departmentName, "");
        }
        for (String noise : List.of("今天", "今日", "明天", "还有没有", "有没有", "能不能", "可不可以",
                "可以", "挂号", "预约", "患者", "医生", "出诊", "排班", "吗", "？", "?", "让")) {
            q = q.replace(noise, "");
        }
        q = q.trim();
        return q.length() >= 2 && q.length() <= 8 ? q : "";
    }

    private String fallbackIntent(String question) {
        if (isScheduleAvailabilityQuestion(question)) {
            return "SCHEDULE";
        }
        if (containsAny(question, "药", "库存", "剩余", "在售", "下架")) {
            return "DRUG";
        }
        if (containsAny(question, "设备", "仪器", "故障", "维护")) {
            return "EQUIPMENT";
        }
        if (containsAny(question, "患者", "病人", "过敏")) {
            return "PATIENT";
        }
        if (containsAny(question, "排班", "出诊", "班次", "号源", "余号")) {
            return "SCHEDULE";
        }
        if (containsAny(question, "挂号", "就诊", "排队", "缴费")) {
            return "REGISTRATION";
        }
        if (containsAny(question, "医生", "工作人员")) {
            return "STAFF";
        }
        return "DASHBOARD";
    }

    private String normalizeIntent(String intent) {
        if (!StringUtils.hasText(intent)) {
            return "DASHBOARD";
        }
        String normalized = intent.trim().toUpperCase(Locale.ROOT);
        return Set.of("DRUG", "REGISTRATION", "EQUIPMENT", "PATIENT", "SCHEDULE", "STAFF", "DASHBOARD")
                .contains(normalized) ? normalized : "DASHBOARD";
    }

    private List<Drug> searchDrugs(String keyword, int limit) {
        String kw = keyword.trim();
        return drugMapper.selectList(
                Wrappers.<Drug>lambdaQuery()
                        .and(w -> w.like(Drug::getDrugName, kw)
                                .or().like(Drug::getDrugCode, kw)
                                .or().like(Drug::getManufacturer, kw))
                        .orderByDesc(Drug::getStatus)
                        .orderByAsc(Drug::getStockQuantity)
                        .last("LIMIT " + limit)
        );
    }

    private void appendDrugTable(StringBuilder sb, List<Drug> drugs) {
        sb.append("| 药品名 | 库存 | 单位 | 状态 |\n|---|---|---|---|\n");
        for (Drug drug : drugs) {
            sb.append("| ").append(drug.getDrugName())
                    .append(" | ").append(drug.getStockQuantity() != null ? drug.getStockQuantity() : 0)
                    .append(" | ").append(nullToDash(drug.getUnit()))
                    .append(" | ").append(drug.getStatus() != null && drug.getStatus() == 1 ? "在售" : "下架")
                    .append(" |\n");
        }
    }

    private String extractDrugKeyword(String question) {
        String q = question.replace("还有多少", "")
                .replace("剩余", "").replace("库存", "")
                .replace("药品", "").replace("药", "")
                .replace("？", "").replace("?", "").trim();
        return q.length() >= 2 ? q : "";
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private record DateRange(LocalDate start, LocalDate end, String label) {
    }

    private record ContextBundle(String context, List<String> sources, String queryType) {
    }
}
