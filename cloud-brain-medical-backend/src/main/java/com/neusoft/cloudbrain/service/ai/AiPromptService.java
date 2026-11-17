package com.neusoft.cloudbrain.service.ai;

import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AiPromptService {

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public String render(String classpathPath, Map<String, String> variables) {
        String template = load(classpathPath);
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    public String load(String classpathPath) {
        return cache.computeIfAbsent(classpathPath, this::readResource);
    }

    private String readResource(String classpathPath) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathPath);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Failed to load prompt: {}", classpathPath, ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "Prompt 文件加载失败: " + classpathPath);
        }
    }
}
