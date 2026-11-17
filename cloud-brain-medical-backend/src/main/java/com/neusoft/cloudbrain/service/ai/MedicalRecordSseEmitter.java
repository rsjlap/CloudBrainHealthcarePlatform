package com.neusoft.cloudbrain.service.ai;

import com.neusoft.cloudbrain.dto.response.medical.AiMedicalDraftVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalRecordSseEmitter {

    public void sendStatus(SseEmitter emitter, String message) {
        sendEvent(emitter, "status", Map.of("message", message));
    }

    public void sendDelta(SseEmitter emitter, String text) {
        sendEvent(emitter, "delta", Map.of("text", text));
    }

    public void sendComplete(SseEmitter emitter, AiMedicalDraftVO draft) {
        sendEvent(emitter, "complete", draft);
    }

    public void sendError(SseEmitter emitter, String message) {
        sendEvent(emitter, "error", Map.of("message", message));
    }

    public void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ex) {
            log.debug("SSE complete ignored: {}", ex.getMessage());
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload));
        } catch (IOException ex) {
            throw new IllegalStateException("SSE 推送失败: " + ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            log.debug("SSE already closed event={}: {}", eventName, ex.getMessage());
        }
    }
}
