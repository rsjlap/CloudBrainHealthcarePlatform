package com.neusoft.cloudbrain.service.ai.speech;

import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.config.SpeechProperties;
import com.neusoft.cloudbrain.dto.response.ai.SpeechTranscribeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechTranscriptionService {

    private final SpeechProperties speechProperties;
    private final IflytekIatClient iflytekIatClient;

    public SpeechTranscribeVO transcribe(MultipartFile file) {
        if (!speechProperties.isEnabled()) {
            throw new BusinessException(ResultCode.AI_UNAVAILABLE, "语音识别未启用，请在配置中开启 app.speech.enabled");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请上传音频文件");
        }
        if (file.getSize() > speechProperties.getMaxFileSizeBytes()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "音频文件过大，请控制在 10MB 以内");
        }

        try {
            byte[] pcm = AudioPcmUtils.toPcm16kMono(file.getBytes(), file.getOriginalFilename());
            int maxBytes = speechProperties.getMaxDurationSeconds() * 16000 * 2;
            if (pcm.length > maxBytes) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "录音时长超过 " + speechProperties.getMaxDurationSeconds() + " 秒限制");
            }
            String text = iflytekIatClient.transcribe(pcm);
            return SpeechTranscribeVO.builder()
                    .text(text)
                    .speechAvailable(true)
                    .build();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Speech transcription failed: {}", ex.getMessage(), ex);
            throw new BusinessException(ResultCode.AI_UNAVAILABLE, toUserMessage(ex));
        }
    }

    private static String toUserMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        if (!StringUtils.hasText(msg)) {
            return "语音识别失败";
        }
        if (msg.contains("Your IP address is not allowed")) {
            return "讯飞 IP 白名单校验失败：请在控制台关闭 IP 白名单，或添加本机公网 IP 后等待约 5 分钟生效";
        }
        if (msg.contains("HMAC signature cannot be verified")) {
            return "讯飞鉴权失败：请检查系统时间是否准确（与网络时间相差不能超过 5 分钟）";
        }
        if (msg.contains("403") || msg.contains("401") || msg.contains("Unauthorized")) {
            return "讯飞鉴权失败，请检查 APPID/APIKey/APISecret 是否正确，"
                    + "以及控制台是否已开通「语音听写」服务。详情: " + msg;
        }
        return msg;
    }

    public SpeechTranscribeVO status() {
        SpeechProperties.Iflytek cfg = speechProperties.getIflytek();
        boolean configured = StringUtils.hasText(cfg.getAppId())
                && StringUtils.hasText(cfg.getApiKey())
                && StringUtils.hasText(cfg.getApiSecret());
        return SpeechTranscribeVO.builder()
                .text("")
                .speechAvailable(speechProperties.isEnabled() && configured)
                .build();
    }
}
