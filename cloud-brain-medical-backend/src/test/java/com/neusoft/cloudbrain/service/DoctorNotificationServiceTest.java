package com.neusoft.cloudbrain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.common.enums.RiskLevel;
import com.neusoft.cloudbrain.dto.response.ai.PrescriptionReviewVO;
import com.neusoft.cloudbrain.dto.websocket.DoctorAlertMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorNotificationService 医生告警推�?)
class DoctorNotificationServiceTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private WebSocketSession session;

    private DoctorNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new DoctorNotificationService(objectMapper);
    }

    @Test
    @DisplayName("正常：注册与注销 WebSocket 会话")
    void registerAndUnregister() {
        notificationService.register(1L, session);
        assertEquals(1, notificationService.onlineDoctorCount());

        notificationService.unregister(1L, session);
        assertEquals(0, notificationService.onlineDoctorCount());
    }

    @Test
    @DisplayName("边界：非 HIGH 风险不推�?)
    void notifyHighRiskPrescription_skipsNonHigh() throws Exception {
        notificationService.register(1L, session);
        PrescriptionReviewVO review = PrescriptionReviewVO.builder()
                .riskLevel(RiskLevel.LOW)
                .build();

        notificationService.notifyHighRiskPrescription(1L, 10L, review);

        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    @DisplayName("正常：HIGH 风险推送告�?)
    void notifyHighRiskPrescription_sendsAlert() throws Exception {
        notificationService.register(1L, session);
        when(session.isOpen()).thenReturn(true);
        when(objectMapper.writeValueAsString(any(DoctorAlertMessage.class))).thenReturn("{\"type\":\"alert\"}");

        PrescriptionReviewVO review = PrescriptionReviewVO.builder()
                .riskLevel(RiskLevel.HIGH)
                .overallAdvice("请复核用�?)
                .warnings(List.of("过敏风险"))
                .build();

        notificationService.notifyHighRiskPrescription(1L, 10L, review);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("边界：无在线会话时不抛异�?)
    void sendToDoctor_noSession() throws Exception {
        DoctorAlertMessage alert = DoctorAlertMessage.builder().type("TEST").build();

        notificationService.sendToDoctor(99L, alert);

        verify(objectMapper, never()).writeValueAsString(any());
    }
}
