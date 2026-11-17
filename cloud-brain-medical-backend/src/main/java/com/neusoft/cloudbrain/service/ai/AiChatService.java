package com.neusoft.cloudbrain.service.ai;

import com.neusoft.cloudbrain.dto.response.ai.AiChatPingVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AiChatService {

    private static final String DEFAULT_PROMPT = "请用一句话回复：DeepSeek 连接成功";

    @Autowired(required = false)
    private ChatClient chatClient;

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String model;

    public AiChatPingVO ping(String message) {
        String prompt = StringUtils.hasText(message) ? message : DEFAULT_PROMPT;

        if (!aiEnabled) {
            return AiChatPingVO.builder()
                    .available(false)
                    .model(model)
                    .prompt(prompt)
                    .error("AI 未启用，请设置环境变量 AI_ENABLED=true")
                    .build();
        }
        if (chatClient == null) {
            return AiChatPingVO.builder()
                    .available(false)
                    .model(model)
                    .prompt(prompt)
                    .error("ChatClient 未就绪，请检查 Spring AI 配置与 API Key")
                    .build();
        }

        try {
            String reply = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.info("DeepSeek ping succeeded, model={}", model);
            return AiChatPingVO.builder()
                    .available(true)
                    .model(model)
                    .prompt(prompt)
                    .reply(reply)
                    .build();
        } catch (Exception ex) {
            log.error("DeepSeek ping failed, model={}", model, ex);
            return AiChatPingVO.builder()
                    .available(false)
                    .model(model)
                    .prompt(prompt)
                    .error(ex.getMessage())
                    .build();
        }
    }
}
