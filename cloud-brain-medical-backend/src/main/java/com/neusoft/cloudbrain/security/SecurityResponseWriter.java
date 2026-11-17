package com.neusoft.cloudbrain.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.common.result.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

final class SecurityResponseWriter {

    private SecurityResponseWriter() {
    }

    static void write(HttpServletResponse response,
                      ObjectMapper objectMapper,
                      ResultCode code,
                      String message) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<Void> result = message != null ? Result.fail(code, message) : Result.fail(code);
        objectMapper.writeValue(response.getWriter(), result);
    }
}
