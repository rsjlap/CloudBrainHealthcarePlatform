package com.neusoft.cloudbrain.service.ai;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.function.Consumer;
import com.neusoft.cloudbrain.common.util.AiJsonResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiLlmClient {

    @Autowired(required = false)
    private ChatClient chatClient;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    private final ObjectMapper objectMapper;

    public boolean isAvailable() {
        return aiEnabled && chatClient != null;
    }

    public String chat(String prompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("AI 未启用或 ChatClient 不可用");
        }
        return chatClient.prompt().user(prompt).call().content();
    }

    public void streamChat(String prompt, Consumer<String> chunkConsumer) {
        if (!isAvailable()) {
            throw new IllegalStateException("AI 未启用或 ChatClient 不可用");
        }
        Flux<String> flux = chatClient.prompt().user(prompt).stream().content();
        flux.doOnNext(chunk -> {
            if (StringUtils.hasText(chunk)) {
                chunkConsumer.accept(chunk);
            }
        }).blockLast();
    }

    public <T> T chatForJson(String prompt, Class<T> responseType) {
        try {
            String json = AiJsonResponseUtil.extractJson(chat(prompt));
            return objectMapper.readValue(json, responseType);
        } catch (Exception ex) {
            throw new IllegalStateException("AI JSON 解析失败: " + ex.getMessage(), ex);
        }
    }

    public <T> List<T> chatForJsonList(String prompt, Class<T> elementType) {
        try {
            String json = AiJsonResponseUtil.extractJson(chat(prompt));
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(json, listType);
        } catch (Exception ex) {
            throw new IllegalStateException("AI JSON 解析失败: " + ex.getMessage(), ex);
        }
    }
}
