package com.neusoft.cloudbrain.service.kg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.config.KgProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphClient {

    private static final List<String> WRAPPER_FIELDS = List.of("data", "result", "payload");
    private static final List<String> CONTENT_FIELDS = List.of(
            "answer", "raw", "result", "content", "text", "response", "reply", "exams", "drugs"
    );
    private static final List<String> FALLBACK_TEXT_FIELDS = List.of("message", "msg");

    private final KgProperties kgProperties;
    private final RestClient kgRestClient;
    private final ObjectMapper objectMapper;

    @Getter
    private volatile String lastError = "";

    public boolean isEnabled() {
        return kgProperties.isEnabled();
    }

    public String getBaseUrl() {
        return kgProperties.getBaseUrl();
    }

    public String getApiMode() {
        return kgProperties.getApiMode();
    }

    public boolean isHealthy() {
        if (!isEnabled()) {
            lastError = "知识图谱未启用（app.kg.enabled=false）";
            return false;
        }
        try {
            String body = kgRestClient.get()
                    .uri(kgProperties.getPaths().getHealth())
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(body)) {
                lastError = "远程 KG 返回空响应";
                return false;
            }
            lastError = "";
            return true;
        } catch (RestClientException ex) {
            lastError = buildConnectionError(ex);
            log.warn("KG health check failed baseUrl={}: {}", kgProperties.getBaseUrl(), lastError);
            return false;
        }
    }

    public Optional<String> chat(String question) {
        return postJson(kgProperties.getPaths().getChat(), Map.of("question", question));
    }

    public Optional<String> queryExam(String disease) {
        return queryByDisease(kgProperties.getPaths().getExam(), disease);
    }

    public Optional<String> queryDrug(String disease) {
        return queryByDisease(kgProperties.getPaths().getDrug(), disease);
    }

    public Optional<String> queryDrugForbidden(String disease) {
        return queryByDisease(kgProperties.getPaths().getForbidden(), disease);
    }

    public Optional<String> validateMedicalTerms(String diagnosis, String terms) {
        String question = "请校验以下病历中的医学术语与疾病名称是否规范、准确，并指出需要修正的术语。"
                + "疾病/诊断：" + defaultText(diagnosis)
                + "；待校验内容：" + defaultText(terms);
        return chat(question);
    }

    private Optional<String> queryByDisease(String pathTemplate, String disease) {
        if (!isEnabled()) {
            lastError = "知识图谱未启用";
            return Optional.empty();
        }
        if (!StringUtils.hasText(disease)) {
            lastError = "疾病名称为空，无法查询知识图谱";
            return Optional.empty();
        }
        if (kgProperties.isRestMode()) {
            return getRest(pathTemplate, disease);
        }
        return postJson(pathTemplate, Map.of("disease", disease.trim()));
    }

    private Optional<String> getRest(String pathTemplate, String disease) {
        try {
            String encoded = UriUtils.encodePathSegment(disease.trim(), StandardCharsets.UTF_8);
            String uri = pathTemplate.replace("{disease}", encoded);
            String raw = kgRestClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            lastError = "";
            return parseResponse(raw);
        } catch (Exception ex) {
            lastError = buildConnectionError(ex);
            log.warn("KG GET failed baseUrl={} path={} disease={}: {}",
                    kgProperties.getBaseUrl(), pathTemplate, disease, lastError);
            return Optional.empty();
        }
    }

    private Optional<String> postJson(String path, Map<String, String> payload) {
        if (!isEnabled()) {
            lastError = "知识图谱未启用";
            return Optional.empty();
        }
        try {
            String raw = kgRestClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            lastError = "";
            return parseResponse(raw);
        } catch (Exception ex) {
            lastError = buildConnectionError(ex);
            log.warn("KG POST failed baseUrl={} path={} payload={}: {}",
                    kgProperties.getBaseUrl(), path, payload, lastError);
            return Optional.empty();
        }
    }

    private String buildConnectionError(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        if (msg.contains("Connection refused") || msg.contains("connect timed out")
                || msg.contains("Failed to connect") || msg.contains("无法连接")) {
            return "无法连接 " + kgProperties.getBaseUrl()
                    + "，请确认：① KG 服务已启动 ② base-url 填的是图谱那台电脑的 IP（不是 127.0.0.1）③ 防火墙已放行端口";
        }
        return msg;
    }

    private Optional<String> parseResponse(String raw) {
        if (!StringUtils.hasText(raw)) {
            lastError = "远程 KG 返回空内容";
            return Optional.empty();
        }
        try {
            String content = extractContent(objectMapper.readTree(raw));
            if (StringUtils.hasText(content)) {
                return Optional.of(content.trim());
            }
            String trimmed = raw.trim();
            if (isPlaceholderStatus(trimmed)) {
                lastError = "远程 KG 仅返回状态信息，无有效内容";
                return Optional.empty();
            }
            return Optional.of(trimmed);
        } catch (Exception ex) {
            String trimmed = raw.trim();
            if (isPlaceholderStatus(trimmed)) {
                lastError = "远程 KG 返回无效内容";
                return Optional.empty();
            }
            return Optional.of(trimmed);
        }
    }

    private String extractContent(JsonNode root) {
        if (root == null || root.isNull()) {
            return "";
        }
        if (root.isTextual()) {
            String text = root.asText();
            return isPlaceholderStatus(text) ? "" : text;
        }
        for (String field : WRAPPER_FIELDS) {
            JsonNode nested = root.get(field);
            if (nested != null && !nested.isNull()) {
                String text = extractContent(nested);
                if (StringUtils.hasText(text)) {
                    return text;
                }
            }
        }
        for (String field : CONTENT_FIELDS) {
            JsonNode node = root.get(field);
            if (node != null && !node.isNull()) {
                String text = nodeToText(node);
                if (StringUtils.hasText(text) && !isPlaceholderStatus(text)) {
                    return text;
                }
            }
        }
        for (String field : FALLBACK_TEXT_FIELDS) {
            JsonNode node = root.get(field);
            if (node != null && !node.isNull()) {
                String text = nodeToText(node);
                if (StringUtils.hasText(text) && !isPlaceholderStatus(text)) {
                    return text;
                }
            }
        }
        return "";
    }

    private boolean isPlaceholderStatus(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        String normalized = text.trim().toLowerCase();
        return normalized.equals("success")
                || normalized.equals("ok")
                || normalized.equals("true")
                || normalized.equals("failed")
                || normalized.equals("fail")
                || normalized.equals("error");
    }

    private String nodeToText(JsonNode node) {
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            node.forEach(item -> {
                String text = nodeToText(item);
                if (StringUtils.hasText(text)) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(text);
                }
            });
            return sb.toString();
        }
        if (node.isObject()) {
            JsonNode name = node.get("name");
            if (name != null && name.isTextual()) {
                return name.asText();
            }
            return node.toString();
        }
        return node.asText("");
    }

    private String defaultText(String text) {
        return StringUtils.hasText(text) ? text.trim() : "未提供";
    }
}
