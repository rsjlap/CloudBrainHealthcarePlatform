package com.neusoft.cloudbrain.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiLlmClient LLM 调用封装")
class AiLlmClientTest {

    private AiLlmClient aiLlmClient;

    @BeforeEach
    void setUp() {
        aiLlmClient = new AiLlmClient(new ObjectMapper());
        ReflectionTestUtils.setField(aiLlmClient, "aiEnabled", false);
        ReflectionTestUtils.setField(aiLlmClient, "chatClient", null);
    }

    @Test
    @DisplayName("边界：AI 不可用时 isAvailable �?false")
    void isAvailable_false() {
        assertFalse(aiLlmClient.isAvailable());
    }

    @Test
    @DisplayName("异常：AI 不可用时 chat �?IllegalStateException")
    void chat_unavailable() {
        assertThrows(IllegalStateException.class, () -> aiLlmClient.chat("test"));
    }

    @Test
    @DisplayName("正常：启用且 ChatClient 就绪")
    void isAvailable_true() {
        ReflectionTestUtils.setField(aiLlmClient, "aiEnabled", true);
        ReflectionTestUtils.setField(aiLlmClient, "chatClient", new Object());

        assertTrue(aiLlmClient.isAvailable());
    }
}
