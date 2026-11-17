package com.neusoft.cloudbrain.websocket;

import com.neusoft.cloudbrain.service.DoctorNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorWebSocketHandler extends TextWebSocketHandler {

    private final DoctorNotificationService doctorNotificationService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        doctorNotificationService.register(userId, session);
        log.info("Doctor WebSocket connected userId={} sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equalsIgnoreCase(message.getPayload().trim())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            doctorNotificationService.unregister(userId, session);
            log.info("Doctor WebSocket closed userId={} sessionId={} status={}",
                    userId, session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Doctor WebSocket transport error sessionId={}: {}",
                session.getId(), exception.getMessage());
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            doctorNotificationService.unregister(userId, session);
        }
        closeQuietly(session);
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.NOT_ACCEPTABLE);
            }
        } catch (Exception ex) {
            log.debug("Failed to close WebSocket session {}", session.getId(), ex);
        }
    }
}
