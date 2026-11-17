package com.neusoft.cloudbrain.service.ai;

import com.neusoft.cloudbrain.dto.response.medical.AiMedicalDraftVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicalRecordSseEmitter SSE 推�?)
class MedicalRecordSseEmitterTest {

    @Mock
    private SseEmitter emitter;

    private MedicalRecordSseEmitter sseEmitter;

    @BeforeEach
    void setUp() {
        sseEmitter = new MedicalRecordSseEmitter();
    }

    @Test
    @DisplayName("正常：发送状态事�?)
    void sendStatus() throws IOException {
        sseEmitter.sendStatus(emitter, "生成�?);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("正常：发送增量文�?)
    void sendDelta() throws IOException {
        sseEmitter.sendDelta(emitter, "片段");

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("正常：发送完成事�?)
    void sendComplete() throws IOException {
        AiMedicalDraftVO draft = AiMedicalDraftVO.builder().chiefComplaint("头痛").build();

        sseEmitter.sendComplete(emitter, draft);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("异常：IO 失败�?IllegalStateException")
    void sendError_ioFailure() throws IOException {
        doThrow(new IOException("closed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        assertThrows(IllegalStateException.class, () -> sseEmitter.sendError(emitter, "错误"));
    }

    @Test
    @DisplayName("边界：completeQuietly 吞掉异常")
    void completeQuietly_swallowsException() throws IOException {
        doThrow(new IOException("closed")).when(emitter).complete();

        assertDoesNotThrow(() -> sseEmitter.completeQuietly(emitter));
    }
}
