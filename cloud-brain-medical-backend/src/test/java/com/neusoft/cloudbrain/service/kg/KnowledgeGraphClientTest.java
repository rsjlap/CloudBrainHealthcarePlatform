package com.neusoft.cloudbrain.service.kg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.cloudbrain.config.KgProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeGraphClient 知识图谱客户�?)
class KnowledgeGraphClientTest {

    @Mock
    private RestClient kgRestClient;

    private KgProperties kgProperties;
    private KnowledgeGraphClient client;

    @BeforeEach
    void setUp() {
        kgProperties = new KgProperties();
        kgProperties.setEnabled(false);
        client = new KnowledgeGraphClient(kgProperties, kgRestClient, new ObjectMapper());
    }

    @Test
    @DisplayName("边界：未启用�?isEnabled �?false")
    void isEnabled_false() {
        assertFalse(client.isEnabled());
    }

    @Test
    @DisplayName("边界：未启用�?queryExam 返回�?)
    void queryExam_disabled() {
        Optional<String> result = client.queryExam("高血�?);

        assertTrue(result.isEmpty());
        assertEquals("知识图谱未启�?, client.getLastError());
    }

    @Test
    @DisplayName("边界：未启用�?isHealthy �?false")
    void isHealthy_disabled() {
        assertFalse(client.isHealthy());
    }

    @Test
    @DisplayName("正常：读取配�?)
    void getters() {
        kgProperties.setBaseUrl("http://192.168.1.1:5000");
        kgProperties.setApiMode("rest");

        assertEquals("http://192.168.1.1:5000", client.getBaseUrl());
        assertEquals("rest", client.getApiMode());
    }
}
