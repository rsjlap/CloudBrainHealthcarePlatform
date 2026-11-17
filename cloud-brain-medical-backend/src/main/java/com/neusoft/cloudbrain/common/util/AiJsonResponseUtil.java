package com.neusoft.cloudbrain.common.util;

public final class AiJsonResponseUtil {

    private AiJsonResponseUtil() {
    }

    public static String extractJson(String text) {
        if (text == null || text.isBlank()) {
            return "{}";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (start >= 0 && end > start) {
                trimmed = trimmed.substring(start + 1, end).trim();
            }
        }
        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        int start;
        if (objectStart >= 0 && (arrayStart < 0 || objectStart < arrayStart)) {
            start = objectStart;
        } else if (arrayStart >= 0) {
            start = arrayStart;
        } else {
            return trimmed;
        }
        char open = trimmed.charAt(start);
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        for (int i = start; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
                if (depth == 0) {
                    return trimmed.substring(start, i + 1);
                }
            }
        }
        return trimmed.substring(start);
    }
}
