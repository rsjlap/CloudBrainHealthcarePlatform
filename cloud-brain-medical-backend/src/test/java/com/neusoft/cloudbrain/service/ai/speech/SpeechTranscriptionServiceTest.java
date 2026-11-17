package com.neusoft.cloudbrain.service.ai.speech;

import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.config.SpeechProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpeechTranscriptionService 语音转写")
class SpeechTranscriptionServiceTest {

    @Mock
    private IflytekIatClient iflytekIatClient;

    private SpeechProperties speechProperties;
    private SpeechTranscriptionService speechTranscriptionService;

    @BeforeEach
    void setUp() {
        speechProperties = new SpeechProperties();
        speechProperties.setEnabled(false);
        speechTranscriptionService = new SpeechTranscriptionService(speechProperties, iflytekIatClient);
    }

    @Test
    @DisplayName("异常：语音识别未启用")
    void transcribe_disabled() {
        MockMultipartFile file = new MockMultipartFile("file", "test.wav", "audio/wav", new byte[]{1, 2});

        BusinessException ex = assertThrows(BusinessException.class,
                () -> speechTranscriptionService.transcribe(file));
        assertEquals(ResultCode.AI_UNAVAILABLE, ex.getResultCode());
    }

    @Test
    @DisplayName("异常：空文件")
    void transcribe_emptyFile() {
        speechProperties.setEnabled(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> speechTranscriptionService.transcribe(null));
        assertEquals(ResultCode.PARAM_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("正常：status 未配置凭�?)
    void status_notConfigured() {
        var status = speechTranscriptionService.status();

        assertFalse(status.isSpeechAvailable());
    }

    @Test
    @DisplayName("正常：status 已配�?)
    void status_configured() {
        speechProperties.setEnabled(true);
        speechProperties.getIflytek().setAppId("app");
        speechProperties.getIflytek().setApiKey("key");
        speechProperties.getIflytek().setApiSecret("secret");

        var status = speechTranscriptionService.status();

        assertTrue(status.isSpeechAvailable());
    }
}
