package com.neusoft.cloudbrain.controller.ai;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.enums.RiskLevel;
import com.neusoft.cloudbrain.common.util.SecurityContextUtil;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.dto.request.ai.ExamRecommendRequest;
import com.neusoft.cloudbrain.dto.request.ai.PrescriptionSuggestRequest;
import com.neusoft.cloudbrain.dto.request.ai.ScheduleGenerateRequest;
import com.neusoft.cloudbrain.dto.request.medical.GenerateMedicalRecordRequest;
import com.neusoft.cloudbrain.dto.request.medical.RefineTreatmentPlanRequest;
import com.neusoft.cloudbrain.dto.request.prescription.ReviewPrescriptionRequest;
import com.neusoft.cloudbrain.dto.response.ai.AiChatPingVO;
import com.neusoft.cloudbrain.dto.response.ai.ExamRecommendVO;
import com.neusoft.cloudbrain.dto.response.ai.ExamReportDraftVO;
import com.neusoft.cloudbrain.dto.response.ai.PrescriptionReviewVO;
import com.neusoft.cloudbrain.dto.response.ai.PrescriptionSuggestVO;
import com.neusoft.cloudbrain.dto.response.ai.AiScheduleGenerateResultVO;
import com.neusoft.cloudbrain.dto.response.ai.SpeechTranscribeVO;
import com.neusoft.cloudbrain.dto.request.kg.KgChatRequest;
import com.neusoft.cloudbrain.dto.response.kg.KgHealthVO;
import com.neusoft.cloudbrain.dto.response.medical.AiMedicalDraftVO;
import com.neusoft.cloudbrain.dto.response.medical.TreatmentPlanRefineVO;
import com.neusoft.cloudbrain.service.DoctorNotificationService;
import com.neusoft.cloudbrain.service.ai.*;
import com.neusoft.cloudbrain.service.ai.speech.SpeechTranscriptionService;
import com.neusoft.cloudbrain.service.kg.KnowledgeGraphClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Tag(name = "AI 能力模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiMedicalRecordService aiMedicalRecordService;
    private final AiPrescriptionService aiPrescriptionService;
    private final AiExamRecommendService aiExamRecommendService;
    private final AiExamReportService aiExamReportService;
    private final AiSchedulingService aiSchedulingService;
    private final AiChatService aiChatService;
    private final KnowledgeGraphClient knowledgeGraphClient;
    private final SpeechTranscriptionService speechTranscriptionService;
    private final DoctorNotificationService doctorNotificationService;

    @Operation(summary = "ChatClient 连通性测试", description = "调用 DeepSeek 验证 ChatClient 是否可用，无需请求体")
    @GetMapping("/ping")
    public Result<AiChatPingVO> pingGet(@RequestParam(required = false) String message) {
        return Result.success(aiChatService.ping(message));
    }

    @PostMapping("/ping")
    public Result<AiChatPingVO> pingPost(@RequestParam(required = false) String message) {
        return Result.success(aiChatService.ping(message));
    }

    @Operation(summary = "AI 生成病历", description = "将患者述说转换为门诊病历页字段 JSON")
    @PostMapping("/medical-record/generate")
    public Result<AiMedicalDraftVO> generateMedicalRecord(@Valid @RequestBody GenerateMedicalRecordRequest request) {
        return Result.success(aiMedicalRecordService.generateMedicalRecord(
                request.getRegistrationId(), request.getDialogText()));
    }

    @Operation(summary = "AI 流式生成病历", description = "SSE 流式输出 LLM 草稿，完成后返回结构化病历 JSON")
    @PostMapping(value = "/medical-record/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateMedicalRecordStream(@Valid @RequestBody GenerateMedicalRecordRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        CompletableFuture.runAsync(() -> aiMedicalRecordService.generateMedicalRecordStream(
                request.getRegistrationId(), request.getDialogText(), emitter));
        return emitter;
    }

    @Operation(summary = "语音识别服务状态")
    @GetMapping("/speech/status")
    public Result<SpeechTranscribeVO> speechStatus() {
        return Result.success(speechTranscriptionService.status());
    }

    @Operation(summary = "语音转文字", description = "上传录音文件，调用科大讯飞听写接口转写为文本")
    @PostMapping("/speech/transcribe")
    public Result<SpeechTranscribeVO> transcribeSpeech(@RequestParam("file") MultipartFile file) {
        return Result.success(speechTranscriptionService.transcribe(file));
    }

    @Operation(summary = "AI 结合检查结果优化处理意见")
    @PostMapping("/medical-record/refine-treatment-plan")
    public Result<TreatmentPlanRefineVO> refineTreatmentPlan(@Valid @RequestBody RefineTreatmentPlanRequest request) {
        return Result.success(aiMedicalRecordService.refineTreatmentPlanWithExamResults(
                request.getRegistrationId(), request.getTreatmentPlan(), request.getDiagnosis()));
    }

    @Operation(summary = "AI 推荐检查", description = "根据主诉、症状和诊断推荐检查类型与部位")
    @PostMapping("/exam/recommend")
    public Result<ExamRecommendVO> recommendExams(@RequestBody ExamRecommendRequest request) {
        return Result.success(aiExamRecommendService.recommendExams(
                request.getChiefComplaint(), request.getSymptoms(), request.getDiagnosis()));
    }

    @Operation(summary = "AI 推荐处方", description = "根据诊断、症状和过敏史推荐药品及风险提示")
    @PostMapping("/prescription/suggest")
    public Result<PrescriptionSuggestVO> suggestPrescription(@Valid @RequestBody PrescriptionSuggestRequest request) {
        return Result.success(aiPrescriptionService.suggestPrescription(
                request.getPatientId(),
                request.getChiefComplaint(),
                request.getSymptoms(),
                request.getDiagnosis()));
    }

    @Operation(summary = "AI 处方审核")
    @PostMapping("/prescription/review")
    public Result<PrescriptionReviewVO> reviewPrescription(@Valid @RequestBody ReviewPrescriptionRequest request) {
        PrescriptionReviewVO review = aiPrescriptionService.reviewPrescription(
                request.getPatientId(), request.getDiagnosis(), request.getItems());
        if (review.getRiskLevel() == RiskLevel.HIGH) {
            doctorNotificationService.notifyHighRiskPrescription(
                    SecurityContextUtil.getCurrentUserId(), request.getPatientId(), review);
        }
        return Result.success(review);
    }

    @Operation(summary = "AI 生成检查报告")
    @PostMapping("/exam-report/generate")
    public Result<ExamReportDraftVO> generateExamReport(@RequestBody Map<String, String> body) {
        return Result.success(aiExamReportService.generateExamReport(
                body.get("examType"), body.get("bodyPart"), body.get("clinicalIndication")));
    }

    @Operation(summary = "AI 生成排班（DeepSeek）")
    @PostMapping("/schedule/generate")
    public Result<AiScheduleGenerateResultVO> generateSchedule(@Valid @RequestBody ScheduleGenerateRequest request) {
        return Result.success(aiSchedulingService.generateSchedule(
                request.getDeptId(), request.getStartDate(), request.getEndDate()));
    }

    @Operation(summary = "知识图谱连通性测试", description = "检测远程 KG 服务是否可达，查看 baseUrl 与 healthy 状态")
    @GetMapping("/kg/health")
    public Result<KgHealthVO> kgHealth() {
        boolean healthy = knowledgeGraphClient.isHealthy();
        return Result.success(KgHealthVO.builder()
                .enabled(knowledgeGraphClient.isEnabled())
                .healthy(healthy)
                .baseUrl(knowledgeGraphClient.getBaseUrl())
                .apiMode(knowledgeGraphClient.getApiMode())
                .message(healthy ? "连接正常" : knowledgeGraphClient.getLastError())
                .build());
    }

    @Operation(summary = "知识图谱通用问答")
    @PostMapping("/kg/chat")
    public Result<Map<String, String>> kgChat(@Valid @RequestBody KgChatRequest request) {
        return knowledgeGraphClient.chat(request.getQuestion())
                .map(answer -> Result.success(Map.of("answer", answer)))
                .orElseGet(() -> Result.success(Map.of(
                        "answer", "",
                        "error", knowledgeGraphClient.getLastError()
                )));
    }

    @Operation(summary = "知识图谱查询疾病检查项")
    @PostMapping("/kg/query/exam")
    public Result<Map<String, String>> kgQueryExam(@RequestParam String disease) {
        return knowledgeGraphClient.queryExam(disease)
                .map(result -> Result.success(Map.of("disease", disease, "result", result)))
                .orElseGet(() -> Result.success(Map.of(
                        "disease", disease,
                        "result", "",
                        "error", knowledgeGraphClient.getLastError()
                )));
    }
}
