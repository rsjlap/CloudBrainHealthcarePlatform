package com.neusoft.cloudbrain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * 开发环境开启；生产环境由 Nginx 同源反代时可关闭。
     */
    private boolean enabled = true;

    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173"
    ));
}
