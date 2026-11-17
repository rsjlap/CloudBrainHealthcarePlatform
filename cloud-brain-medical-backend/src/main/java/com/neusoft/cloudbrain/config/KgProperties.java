package com.neusoft.cloudbrain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.kg")
public class KgProperties {

    /**
     * 是否启用知识图谱融合（false 时仅使用大模型，不发起远程 HTTP 调用）
     */
    private boolean enabled = false;

    /**
     * 知识图谱服务根地址，部署在另一台机器时填对方 IP，如 http://192.168.1.100:5000
     */
    private String baseUrl = "http://127.0.0.1:5000";

    /**
     * 接口风格：legacy=原项目 POST /kg/query/*；rest=GET /api/graph/disease/{disease}/*
     */
    private String apiMode = "legacy";

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 20000;

    private Paths paths = new Paths();

    @Data
    public static class Paths {
        private String health = "/health";
        private String chat = "/kg/chat";
        /** legacy: POST body；rest: GET，{disease} 为路径占位符 */
        private String exam = "/kg/query/exam";
        private String drug = "/kg/query/drug";
        private String forbidden = "/kg/query/drug-forbidden";
    }

    public boolean isRestMode() {
        return "rest".equalsIgnoreCase(apiMode);
    }
}
