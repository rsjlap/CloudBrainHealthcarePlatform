package com.neusoft.cloudbrain.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws java.io.IOException {
        if (request.getHeader("Authorization") == null) {
            SecurityResponseWriter.write(response, objectMapper, ResultCode.UNAUTHORIZED,
                    "请先登录并在 Authorize 中配置 Bearer token");
        } else {
            SecurityResponseWriter.write(response, objectMapper, ResultCode.FORBIDDEN, null);
        }
    }
}
