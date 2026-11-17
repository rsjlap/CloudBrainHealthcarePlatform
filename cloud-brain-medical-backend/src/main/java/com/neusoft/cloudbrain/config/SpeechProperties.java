package com.neusoft.cloudbrain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.speech")
public class SpeechProperties {

    private boolean enabled = false;

    private String provider = "iflytek";

    private int maxDurationSeconds = 180;

    private long maxFileSizeBytes = 10 * 1024 * 1024;

    private Iflytek iflytek = new Iflytek();

    @Data
    public static class Iflytek {
        private String appId = "";
        private String apiKey = "";
        private String apiSecret = "";
        private String host = "iat-api.xfyun.cn";
        private String path = "/v2/iat";
    }
}
