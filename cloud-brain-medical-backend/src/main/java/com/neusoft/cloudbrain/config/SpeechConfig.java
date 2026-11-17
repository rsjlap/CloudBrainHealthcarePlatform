package com.neusoft.cloudbrain.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SpeechProperties.class)
public class SpeechConfig {

    private final SpeechProperties speechProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void logSpeechConfig(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String[] profiles = env.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = env.getDefaultProfiles();
        }

        if (!speechProperties.isEnabled()) {
            log.info("Speech transcription disabled (profiles={})", Arrays.toString(profiles));
            return;
        }

        SpeechProperties.Iflytek cfg = speechProperties.getIflytek();
        boolean configured = StringUtils.hasText(cfg.getAppId())
                && StringUtils.hasText(cfg.getApiKey())
                && StringUtils.hasText(cfg.getApiSecret());
        if (!configured) {
            log.warn("Speech transcription enabled but iFlytek credentials are missing (profiles={}). "
                            + "Check IFLYTEK_APP_ID / IFLYTEK_API_KEY / IFLYTEK_API_SECRET env vars.",
                    Arrays.toString(profiles));
            return;
        }

        log.info("Speech transcription ready (profiles={}, appId={}, apiKey={}..., apiSecret={}...)",
                Arrays.toString(profiles),
                mask(cfg.getAppId()),
                maskPrefix(cfg.getApiKey()),
                maskPrefix(cfg.getApiSecret()));
    }

    private static String mask(String value) {
        if (!StringUtils.hasText(value) || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 4) + "****";
    }

    private static String maskPrefix(String value) {
        if (!StringUtils.hasText(value) || value.length() <= 6) {
            return "****";
        }
        return value.substring(0, 6) + "...";
    }
}
