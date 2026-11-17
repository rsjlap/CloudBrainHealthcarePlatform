package com.neusoft.cloudbrain.common.util;

import com.neusoft.cloudbrain.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JwtUtil Token 工具")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("unit-test-jwt-secret-key-256bits-minimum!!");
        config.setExpirationMs(3_600_000L);
        jwtUtil = new JwtUtil(config);
    }

    @Test
    @DisplayName("正常：生成并解析 Token")
    void generateAndParseToken() {
        String token = jwtUtil.generateToken(2L, "doctor1", "DOCTOR");
        assertNotNull(token);
        assertTrue(jwtUtil.isValid(token));
        assertEquals(2L, jwtUtil.getUserId(token));
        assertEquals("doctor1", jwtUtil.getUsername(token));
        assertEquals("DOCTOR", jwtUtil.getRole(token));
    }

    @Test
    @DisplayName("异常：非法 Token 校验失败")
    void isValid_invalidToken() {
        assertFalse(jwtUtil.isValid("invalid.token.value"));
        assertFalse(jwtUtil.isValid(""));
    }
}
