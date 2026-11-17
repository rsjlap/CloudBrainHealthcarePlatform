package com.neusoft.cloudbrain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.common.enums.RiskLevel;
import com.neusoft.cloudbrain.dto.response.ai.PrescriptionReviewVO;
import com.neusoft.cloudbrain.dto.websocket.DoctorAlertMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorNotificationService {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessionsByDoctor = new ConcurrentHashMap<>();

    public void register(Long doctorId, WebSocketSession session) {
        sessionsByDoctor.computeIfAbsent(doctorId, ignored -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unregister(Long doctorId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByDoctor.get(doctorId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByDoctor.remove(doctorId, sessions);
        }
    }

    public int onlineDoctorCount() {
        return sessionsByDoctor.size();
    }

    public void notifyHighRiskPrescription(Long doctorId, Long patientId, PrescriptionReviewVO review) {
        if (doctorId == null || review == null || review.getRiskLevel() != RiskLevel.HIGH) {
            return;
        }
        DoctorAlertMessage alert = DoctorAlertMessage.builder()
                .type("PRESCRIPTION_HIGH_RISK")
                .title("高风险用药告警")
                .riskLevel(review.getRiskLevel())
                .message("AI 处方审核发现高风险用药，请立即关注")
                .overallAdvice(review.getOverallAdvice())
                .patientId(patientId)
                .warnings(review.getWarnings())
                .timestamp(LocalDateTime.now())
                .build();
        sendToDoctor(doctorId, alert);
    }

    public void sendToDoctor(Long doctorId, DoctorAlertMessage alert) {
        Set<WebSocketSession> sessions = sessionsByDoctor.get(doctorId);
        if (sessions == null || sessions.isEmpty()) {
            log.info("No active WebSocket session for doctorId={}, alertType={}",
                    doctorId, alert.getType());
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(alert);
            TextMessage message = new TextMessage(payload);
            for (WebSocketSession session : sessions) {
                sendSafely(session, message);
            }
            log.info("Pushed alert to doctorId={} type={} sessions={}",
                    doctorId, alert.getType(), sessions.size());
        } catch (Exception ex) {
            log.warn("Failed to serialize doctor alert doctorId={}: {}", doctorId, ex.getMessage());
        }
    }

    private void sendSafely(WebSocketSession session, TextMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException ex) {
            log.warn("Failed to send WebSocket message sessionId={}: {}",
                    session.getId(), ex.getMessage());
        }
    }
}
