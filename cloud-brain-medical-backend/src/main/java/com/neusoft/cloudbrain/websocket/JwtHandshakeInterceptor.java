package com.neusoft.cloudbrain.websocket;

import com.neusoft.cloudbrain.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (!StringUtils.hasText(token) || !jwtUtil.isValid(token)) {
            log.warn("WebSocket handshake rejected: invalid token");
            return false;
        }
        String role = jwtUtil.getRole(token);
        if (!"DOCTOR".equalsIgnoreCase(role)) {
            log.warn("WebSocket handshake rejected: role={}", role);
            return false;
        }
        attributes.put("userId", jwtUtil.getUserId(token));
        attributes.put("username", jwtUtil.getUsername(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}
