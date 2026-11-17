package com.neusoft.cloudbrain.service.ai.speech;

import com.neusoft.cloudbrain.config.SpeechProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("IflytekIatClient 科大讯飞客户端")
class IflytekIatClientTest {

    private SpeechProperties speechProperties;
    private IflytekIatClient iflytekIatClient;

    @BeforeEach
    void setUp() {
        speechProperties = new SpeechProperties();
        iflytekIatClient = new IflytekIatClient(speechProperties);
    }

    @Test
    @DisplayName("异常：空音频")
    void transcribe_emptyPcm() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> iflytekIatClient.transcribe(new byte[0]));
        assertEquals("音频内容为空", ex.getMessage());
    }

    @Test
    @DisplayName("异常：凭证未配置")
    void transcribe_missingCredentials() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> iflytekIatClient.transcribe(new byte[]{1, 2, 3}));
        assertEquals("科大讯飞 APPID / APIKey / APISecret 未配置", ex.getMessage());
    }
}
