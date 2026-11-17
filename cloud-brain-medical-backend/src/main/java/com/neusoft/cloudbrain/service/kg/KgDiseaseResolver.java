package com.neusoft.cloudbrain.service.kg;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class KgDiseaseResolver {

    private static final Pattern TRAILING_PUNCT = Pattern.compile("[？?。；;，,、\\s]+$");
    private static final Pattern LEADING_NOISE = Pattern.compile("^(初步诊断|诊断|疑似|考虑|可能为|可能|待查)[:：\\s]*");

    public String resolve(String... candidates) {
        for (String candidate : candidates) {
            String normalized = normalize(candidate);
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return "";
    }

    private String normalize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String value = text.trim();
        value = LEADING_NOISE.matcher(value).replaceAll("");
        value = TRAILING_PUNCT.matcher(value).replaceAll("");
        if (value.contains("？")) {
            value = value.substring(0, value.indexOf('？')).trim();
        }
        if (value.contains("?")) {
            value = value.substring(0, value.indexOf('?')).trim();
        }
        if (value.contains("，")) {
            value = value.substring(0, value.indexOf('，')).trim();
        }
        if (value.contains(",")) {
            value = value.substring(0, value.indexOf(',')).trim();
        }
        if (value.length() > 30) {
            value = value.substring(0, 30);
        }
        return value;
    }
}
