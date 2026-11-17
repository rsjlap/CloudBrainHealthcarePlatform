package com.neusoft.cloudbrain.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("AiChatService AI 连通性检�?)
class AiChatServiceTest {

    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        aiChatService = new AiChatService();
        ReflectionTestUtils.setField(aiChatService, "aiEnabled", false);
        ReflectionTestUtils.setField(aiChatService, "model", "deepseek-chat");
        ReflectionTestUtils.setField(aiChatService, "chatClient", null);
    }

    @Test
    @DisplayName("边界：AI 未启�?)
    void ping_aiDisabled() {
        var result = aiChatService.ping("hello");

        assertFalse(result.isAvailable());
        assertEquals("deepseek-chat", result.getModel());
    }

    @Test
    @DisplayName("边界：ChatClient 未就�?)
    void ping_chatClientNull() {
        ReflectionTestUtils.setField(aiChatService, "aiEnabled", true);

        var result = aiChatService.ping(null);

        assertFalse(result.isAvailable());
    }
}
