package com.neusoft.cloudbrain.service.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.common.enums.ExamOrderStatus;
import com.neusoft.cloudbrain.common.constant.AiPromptConstants;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.AiJsonResponseUtil;
import com.neusoft.cloudbrain.common.util.PaymentGuard;
import com.neusoft.cloudbrain.dto.response.medical.AiMedicalDraftVO;
import com.neusoft.cloudbrain.dto.response.medical.RecommendedExamVO;
import com.neusoft.cloudbrain.dto.response.medical.TreatmentPlanRefineVO;
import com.neusoft.cloudbrain.entity.ExamOrder;
import com.neusoft.cloudbrain.entity.ExamReport;
import com.neusoft.cloudbrain.entity.Patient;
import com.neusoft.cloudbrain.entity.Registration;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.ExamOrderMapper;
import com.neusoft.cloudbrain.mapper.ExamReportMapper;
import com.neusoft.cloudbrain.mapper.PatientMapper;
import com.neusoft.cloudbrain.mapper.RegistrationMapper;
import com.neusoft.cloudbrain.mapper.UserMapper;
import com.neusoft.cloudbrain.service.ExamCatalogService;
import com.neusoft.cloudbrain.service.kg.KnowledgeGraphClient;
import com.neusoft.cloudbrain.service.kg.KgDiseaseResolver;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMedicalRecordService {

    private final RegistrationMapper registrationMapper;
    private final PatientMapper patientMapper;
    private final UserMapper userMapper;
    private final ExamOrderMapper examOrderMapper;
    private final ExamReportMapper examReportMapper;
    private final ObjectMapper objectMapper;
    private final AiPromptService aiPromptService;
    private final AiLlmClient aiLlmClient;
    private final KnowledgeGraphClient knowledgeGraphClient;
    private final KgDiseaseResolver kgDiseaseResolver;
    private final ExamCatalogService examCatalogService;
    private final MedicalRecordSseEmitter medicalRecordSseEmitter;

    public void generateMedicalRecordStream(Long registrationId, String dialogText, SseEmitter emitter) {
        AtomicBoolean completed = new AtomicBoolean(false);
        try {
            if (!StringUtils.hasText(dialogText)) {
                medicalRecordSseEmitter.sendError(emitter, "问诊对话不能为空");
                return;
            }
            Registration registration = registrationMapper.selectById(registrationId);
            if (registration == null) {
                medicalRecordSseEmitter.sendError(emitter, "挂号记录不存在");
                return;
            }
            PaymentGuard.requireRegistrationPaid(registration);
            Patient patient = patientMapper.selectById(registration.getPatientId());

            if (aiLlmClient.isAvailable()) {
                streamWithLlm(patient, registration, dialogText.trim(), emitter, completed);
                return;
            }

            medicalRecordSseEmitter.sendStatus(emitter, "AI 未启用，正在使用规则引擎生成…");
            AiMedicalDraftVO draft = enrichDraftExams(generateWithRules(patient, registration, dialogText.trim()));
            medicalRecordSseEmitter.sendComplete(emitter, draft);
            completed.set(true);
        } catch (BusinessException ex) {
            if (!completed.get()) {
                medicalRecordSseEmitter.sendError(emitter, ex.getMessage());
            }
        } catch (Exception ex) {
            if (completed.get()) {
                log.debug("Stream medical record finished with post-complete noise registrationId={}", registrationId, ex);
            } else {
                log.warn("Stream medical record failed registrationId={}", registrationId, ex);
                medicalRecordSseEmitter.sendError(emitter, "AI 病历生成失败，请稍后重试");
            }
        } finally {
            medicalRecordSseEmitter.completeQuietly(emitter);
        }
    }

    private void streamWithLlm(Patient patient,
                               Registration registration,
                               String dialogText,
                               SseEmitter emitter,
                               AtomicBoolean completed) {
        String prompt = aiPromptService.render(AiPromptConstants.MEDICAL_RECORD, Map.of(
                "patientInfo", buildPatientInfo(patient, registration),
                "dialogText", dialogText,
                "catalog", examCatalogService.buildCatalogPromptText()
        ));

        medicalRecordSseEmitter.sendStatus(emitter, "AI 正在生成病历草稿…");
        StringBuilder fullText = new StringBuilder();
        try {
            aiLlmClient.streamChat(prompt, chunk -> {
                fullText.append(chunk);
                medicalRecordSseEmitter.sendDelta(emitter, chunk);
            });
        } catch (Exception ex) {
            log.warn("LLM stream failed registrationId={}, fallback to rules", registration.getId(), ex);
            medicalRecordSseEmitter.sendStatus(emitter, "流式生成失败，正在切换规则引擎…");
            AiMedicalDraftVO draft = enrichDraftExams(generateWithRules(patient, registration, dialogText));
            medicalRecordSseEmitter.sendComplete(emitter, draft);
            completed.set(true);
            return;
        }

        AiMedicalDraftVO draft;
        try {
            draft = parseDraftFromLlm(fullText.toString(), patient);
        } catch (Exception ex) {
            log.warn("Parse streamed medical record failed registrationId={}", registration.getId(), ex);
            medicalRecordSseEmitter.sendStatus(emitter, "解析失败，正在切换规则引擎…");
            draft = generateWithRules(patient, registration, dialogText);
        }

        medicalRecordSseEmitter.sendStatus(emitter, "正在融合知识图谱校验术语…");
        AiMedicalDraftVO refined = refineMedicalRecordWithKg(draft, patient, registration, dialogText);
        AiMedicalDraftVO finalDraft = enrichDraftExams(refined);
        medicalRecordSseEmitter.sendComplete(emitter, finalDraft);
        completed.set(true);
        log.info("AI medical record streamed via LLM+KG registrationId={}", registration.getId());
    }

    private AiMedicalDraftVO parseDraftFromLlm(String raw, Patient patient) throws JsonProcessingException {
        String json = AiJsonResponseUtil.extractJson(raw);
        AiMedicalDraftVO draft = objectMapper.readValue(json, AiMedicalDraftVO.class);
        draft.setAiGenerated(true);
        draft.setAiAvailable(true);
        if (!StringUtils.hasText(draft.getAllergyHistory()) && patient != null) {
            draft.setAllergyHistory(StringUtils.hasText(patient.getAllergyHistory())
                    ? patient.getAllergyHistory() : "无");
        }
        return draft;
    }

    public AiMedicalDraftVO generateMedicalRecord(Long registrationId, String dialogText) {
        if (!StringUtils.hasText(dialogText)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "问诊对话不能为空");
        }
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "挂号记录不存在");
        }
        PaymentGuard.requireRegistrationPaid(registration);
        Patient patient = patientMapper.selectById(registration.getPatientId());

        if (aiLlmClient.isAvailable()) {
            try {
                AiMedicalDraftVO draft = generateWithLlm(patient, registration, dialogText);
                AiMedicalDraftVO refined = refineMedicalRecordWithKg(draft, patient, registration, dialogText);
                log.info("AI medical record generated via LLM+KG registrationId={}", registrationId);
                return enrichDraftExams(refined);
            } catch (Exception ex) {
                log.warn("LLM medical record failed, fallback to rules registrationId={}", registrationId, ex);
            }
        }

        AiMedicalDraftVO draft = generateWithRules(patient, registration, dialogText);
        log.info("AI medical record generated via rules registrationId={}", registrationId);
        return enrichDraftExams(draft);
    }

    public TreatmentPlanRefineVO refineTreatmentPlanWithExamResults(Long registrationId,
                                                                    String currentPlan,
                                                                    String diagnosis) {
        try {
            return doRefineTreatmentPlanWithExamResults(registrationId, currentPlan, diagnosis);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Treatment plan refine failed registrationId={}", registrationId, ex);
            String plan = StringUtils.hasText(currentPlan) ? currentPlan.trim() : "";
            return TreatmentPlanRefineVO.builder()
                    .treatmentPlan(plan)
                    .aiAvailable(false)
                    .build();
        }
    }

    private TreatmentPlanRefineVO doRefineTreatmentPlanWithExamResults(Long registrationId,
                                                                       String currentPlan,
                                                                       String diagnosis) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "挂号记录不存在");
        }
        PaymentGuard.requireRegistrationPaid(registration);

        String examResults = buildExamResultsSummary(registrationId);
        if (!StringUtils.hasText(examResults)) {
            return TreatmentPlanRefineVO.builder()
                    .treatmentPlan(StringUtils.hasText(currentPlan) ? currentPlan.trim() : "")
                    .aiAvailable(aiLlmClient.isAvailable())
                    .build();
        }

        String plan = StringUtils.hasText(currentPlan) ? currentPlan.trim() : "暂无初步处理意见，请结合检查结果给出处理建议。";
        String diag = StringUtils.hasText(diagnosis) ? diagnosis.trim()
                : (StringUtils.hasText(registration.getChiefComplaint()) ? registration.getChiefComplaint().trim() : "待查");
        String chiefComplaint = StringUtils.hasText(registration.getChiefComplaint())
                ? registration.getChiefComplaint().trim() : "暂无";

        if (aiLlmClient.isAvailable()) {
            try {
                String prompt = aiPromptService.render(AiPromptConstants.TREATMENT_PLAN_EXAM_REFINE, Map.of(
                        "chiefComplaint", chiefComplaint,
                        "diagnosis", diag,
                        "currentTreatmentPlan", plan,
                        "examResults", examResults
                ));
                TreatmentPlanLlmResult result = aiLlmClient.chatForJson(prompt, TreatmentPlanLlmResult.class);
                if (result != null && StringUtils.hasText(result.getTreatmentPlan())) {
                    log.info("AI treatment plan refined registrationId={}", registrationId);
                    return TreatmentPlanRefineVO.builder()
                            .treatmentPlan(result.getTreatmentPlan().trim())
                            .aiAvailable(true)
                            .build();
                }
            } catch (Exception ex) {
                log.warn("LLM treatment plan refine failed registrationId={}", registrationId, ex);
            }
        }

        return TreatmentPlanRefineVO.builder()
                .treatmentPlan(refineTreatmentPlanWithRules(plan, examResults))
                .aiAvailable(false)
                .build();
    }

    private String buildExamResultsSummary(Long registrationId) {
        List<ExamOrder> orders = examOrderMapper.selectList(
                Wrappers.<ExamOrder>lambdaQuery()
                        .eq(ExamOrder::getRegistrationId, registrationId)
                        .eq(ExamOrder::getStatus, ExamOrderStatus.DONE)
        );
        if (orders.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (ExamOrder order : orders) {
            ExamReport report = examReportMapper.selectOne(
                    Wrappers.<ExamReport>lambdaQuery().eq(ExamReport::getOrderId, order.getId())
            );
            if (report == null) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append("【").append(order.getExamType()).append("】");
            if (StringUtils.hasText(order.getBodyPart())) {
                sb.append("（").append(order.getBodyPart().trim()).append("）");
            }
            sb.append('\n');
            if (StringUtils.hasText(report.getReportText())) {
                sb.append("检查数据：").append(report.getReportText().trim()).append('\n');
            }
            if (StringUtils.hasText(report.getConclusion())) {
                sb.append("结论：").append(report.getConclusion().trim());
            }
        }
        return sb.toString().trim();
    }

    private String refineTreatmentPlanWithRules(String currentPlan, String examResults) {
        return currentPlan + "\n\n【结合检查结果调整意见】\n" + examResults;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TreatmentPlanLlmResult {
        private String treatmentPlan;
    }

    private AiMedicalDraftVO enrichDraftExams(AiMedicalDraftVO draft) {
        if (draft.getRecommendedExams() != null) {
            draft.setRecommendedExams(examCatalogService.retainCatalogMatched(draft.getRecommendedExams()));
        }
        return draft;
    }

    private AiMedicalDraftVO generateWithLlm(Patient patient, Registration registration, String dialogText) {
        String prompt = aiPromptService.render(AiPromptConstants.MEDICAL_RECORD, Map.of(
                "patientInfo", buildPatientInfo(patient, registration),
                "dialogText", dialogText.trim(),
                "catalog", examCatalogService.buildCatalogPromptText()
        ));
        try {
            return parseDraftFromLlm(aiLlmClient.chat(prompt), patient);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("AI JSON 解析失败: " + ex.getMessage(), ex);
        }
    }

    private AiMedicalDraftVO refineMedicalRecordWithKg(AiMedicalDraftVO draft,
                                                       Patient patient,
                                                       Registration registration,
                                                       String dialogText) {
        draft.setAiGenerated(true);
        draft.setAiAvailable(true);

        String disease = kgDiseaseResolver.resolve(
                draft.getDiagnosis(), registration.getChiefComplaint(), draft.getChiefComplaint());
        String terms = buildTermsForValidation(draft);
        Optional<String> kgValidation = knowledgeGraphClient.validateMedicalTerms(disease, terms);
        if (kgValidation.isEmpty()) {
            draft.setKgAvailable(false);
            return enrichDraftExams(draft);
        }

        draft.setKgAvailable(true);
        draft.setKgReference(truncate(kgValidation.get()));
        try {
            String prompt = aiPromptService.render(AiPromptConstants.MEDICAL_RECORD_KG_REFINE, Map.of(
                    "patientInfo", buildPatientInfo(patient, registration),
                    "dialogText", dialogText.trim(),
                    "aiDraft", toAiDraftJson(draft),
                    "kgValidationResult", kgValidation.get(),
                    "catalog", examCatalogService.buildCatalogPromptText()
            ));
            AiMedicalDraftVO refined = aiLlmClient.chatForJson(prompt, AiMedicalDraftVO.class);
            refined.setAiGenerated(true);
            refined.setAiAvailable(true);
            refined.setKgAvailable(true);
            refined.setKgReference(draft.getKgReference());
            if (!StringUtils.hasText(refined.getAllergyHistory()) && patient != null) {
                refined.setAllergyHistory(StringUtils.hasText(patient.getAllergyHistory())
                        ? patient.getAllergyHistory() : "无");
            }
            return enrichDraftExams(refined);
        } catch (Exception ex) {
            log.warn("KG medical record refine failed registrationId={}", registration.getId(), ex);
            return enrichDraftExams(draft);
        }
    }

    private String buildTermsForValidation(AiMedicalDraftVO draft) {
        StringBuilder sb = new StringBuilder();
        appendLine(sb, "主诉", draft.getChiefComplaint());
        appendLine(sb, "初步诊断", draft.getDiagnosis());
        appendLine(sb, "治疗意见", draft.getTreatmentPlan());
        appendLine(sb, "体格检查", draft.getPhysicalExam());
        if (draft.getRecommendedExams() != null) {
            draft.getRecommendedExams().forEach(exam ->
                    appendLine(sb, "建议检查", exam.getExamType() + "/" + exam.getBodyPart()));
        }
        return sb.isEmpty() ? "未提供" : sb.toString().trim();
    }

    private void appendLine(StringBuilder sb, String label, String value) {
        if (StringUtils.hasText(value)) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(label).append("：").append(value.trim());
        }
    }

    private String truncate(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500) + "...";
    }

    private AiMedicalDraftVO generateWithRules(Patient patient, Registration registration, String dialogText) {
        String allergyHistory = patient != null ? patient.getAllergyHistory() : null;
        String medicalHistory = patient != null ? patient.getMedicalHistory() : null;
        String regComplaint = registration.getChiefComplaint();

        String context = buildContext(dialogText, regComplaint);
        RecordTemplate template = resolveTemplate(context);

        String chiefComplaint = firstNonBlank(extractChiefComplaint(dialogText, regComplaint), template.chiefComplaint());
        String presentIllness = buildPresentIllness(dialogText, template);
        String pastHistory = buildPastHistory(allergyHistory, medicalHistory);

        return AiMedicalDraftVO.builder()
                .chiefComplaint(chiefComplaint)
                .presentIllness(presentIllness)
                .pastHistory(pastHistory)
                .allergyHistory(StringUtils.hasText(allergyHistory) ? allergyHistory : "无")
                .personalHistory("无特殊")
                .maritalHistory("无特殊")
                .familyHistory("无特殊")
                .physicalExam(template.physicalExam())
                .diagnosis(template.diagnosis())
                .treatmentPlan(template.treatmentPlan())
                .recommendedExams(examCatalogService.recommendFromContext(
                        context + " " + template.diagnosis(), 4))
                .aiGenerated(true)
                .aiAvailable(false)
                .kgAvailable(false)
                .build();
    }

    public String toAiDraftJson(AiMedicalDraftVO draft) {
        try {
            return objectMapper.writeValueAsString(draft);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "AI 草稿序列化失败");
        }
    }

    private String buildPatientInfo(Patient patient, Registration registration) {
        StringBuilder sb = new StringBuilder();
        if (registration.getChiefComplaint() != null) {
            sb.append("挂号主诉：").append(registration.getChiefComplaint()).append('\n');
        }
        if (patient != null) {
            User user = userMapper.selectById(patient.getId());
            if (user != null && StringUtils.hasText(user.getRealName())) {
                sb.append("姓名：").append(user.getRealName()).append('\n');
            }
            if (StringUtils.hasText(patient.getAllergyHistory())) {
                sb.append("过敏史：").append(patient.getAllergyHistory()).append('\n');
            }
            if (StringUtils.hasText(patient.getMedicalHistory())) {
                sb.append("既往史：").append(patient.getMedicalHistory()).append('\n');
            }
        }
        return sb.isEmpty() ? "暂无" : sb.toString().trim();
    }

    private String buildContext(String dialogText, String regComplaint) {
        StringBuilder sb = new StringBuilder(dialogText.trim());
        if (StringUtils.hasText(regComplaint)) {
            sb.append(' ').append(regComplaint.trim());
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private RecordTemplate resolveTemplate(String context) {
        if (containsAny(context, "胸闷", "胸痛", "气短", "心悸", "心慌", "心前区")) {
            return RecordTemplate.CARDIO;
        }
        if (containsAny(context, "咳嗽", "发热", "咽痛", "流涕", "咳痰", "发烧")) {
            return RecordTemplate.RESPIRATORY;
        }
        if (containsAny(context, "头痛", "头晕", "眩晕")) {
            return RecordTemplate.NEURO;
        }
        return RecordTemplate.GENERAL;
    }

    private String extractChiefComplaint(String dialogText, String regComplaint) {
        if (StringUtils.hasText(regComplaint)) {
            return regComplaint.trim();
        }
        String text = dialogText.trim();
        if (text.length() <= 40) {
            return text;
        }
        int end = text.indexOf('。');
        if (end > 0 && end <= 40) {
            return text.substring(0, end);
        }
        return text.substring(0, Math.min(40, text.length())) + "...";
    }

    private String buildPresentIllness(String dialogText, RecordTemplate template) {
        return "患者因「" + template.chiefComplaint() + "」就诊。"
                + " 问诊记录：" + dialogText.trim()
                + "。 症状特点：" + template.symptomSummary();
    }

    private String buildPastHistory(String allergyHistory, String medicalHistory) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(medicalHistory)) {
            sb.append(medicalHistory.trim());
        } else {
            sb.append("否认高血压、糖尿病等慢性病史。");
        }
        return sb.toString();
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private enum RecordTemplate {
        CARDIO("胸闷气短3天", "活动后胸闷气短加重，伴偶发心悸，休息可缓解。",
                "血压 128/82mmHg，心率 88次/分，律齐，双肺呼吸音清。",
                "冠状动脉粥样硬化性心脏病？心绞痛？",
                "建议完善心电图、心肌酶检查；低盐低脂饮食，避免剧烈活动，必要时给予扩冠治疗。"),
        RESPIRATORY("咳嗽发热2天", "咳嗽伴低热，偶有咽痛，无明显呼吸困难。",
                "体温 37.8℃，咽部充血，双肺呼吸音粗，未闻及干湿啰音。",
                "急性上呼吸道感染",
                "建议多饮水休息，对症退热止咳；如症状加重及时复诊。"),
        NEURO("头痛头晕1天", "头痛以额颞部胀痛为主，伴轻度头晕，无恶心呕吐。",
                "神志清楚，颈软，神经系统查体未见明显阳性体征。",
                "紧张性头痛？",
                "建议规律作息，避免熬夜；必要时给予止痛对症处理，症状持续需进一步头颅检查。"),
        GENERAL("身体不适待查", "患者自述近期身体不适，需结合问诊进一步评估。",
                "一般情况可，生命体征平稳，心肺腹查体未见明显异常。",
                "待进一步检查明确",
                "建议完善基础检查，根据结果调整治疗方案，不适随诊。");

        private final String chiefComplaint;
        private final String symptomSummary;
        private final String physicalExam;
        private final String diagnosis;
        private final String treatmentPlan;

        RecordTemplate(String chiefComplaint, String symptomSummary, String physicalExam,
                       String diagnosis, String treatmentPlan) {
            this.chiefComplaint = chiefComplaint;
            this.symptomSummary = symptomSummary;
            this.physicalExam = physicalExam;
            this.diagnosis = diagnosis;
            this.treatmentPlan = treatmentPlan;
        }

        String chiefComplaint() {
            return chiefComplaint;
        }

        String symptomSummary() {
            return symptomSummary;
        }

        String physicalExam() {
            return physicalExam;
        }

        String diagnosis() {
            return diagnosis;
        }

        String treatmentPlan() {
            return treatmentPlan;
        }
    }
}
