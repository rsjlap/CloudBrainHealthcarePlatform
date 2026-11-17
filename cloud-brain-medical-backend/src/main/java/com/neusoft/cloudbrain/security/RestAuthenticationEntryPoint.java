package com.neusoft.cloudbrain.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws java.io.IOException {
        SecurityResponseWriter.write(response, objectMapper, ResultCode.UNAUTHORIZED,
                "请先登录并在 Authorize 中配置 Bearer token");
    }
}
