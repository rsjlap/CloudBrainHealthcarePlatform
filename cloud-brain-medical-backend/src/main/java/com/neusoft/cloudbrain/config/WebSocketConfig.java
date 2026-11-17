package com.neusoft.cloudbrain.config;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.websocket.DoctorWebSocketHandler;
import com.neusoft.cloudbrain.websocket.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final DoctorWebSocketHandler doctorWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(doctorWebSocketHandler, ApiConstants.BASE_URL + "/ws/doctor")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
