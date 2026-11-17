package com.neusoft.cloudbrain.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PasswordUtil 密码加密")
class PasswordUtilTest {

    private PasswordUtil passwordUtil;

    @BeforeEach
    void setUp() {
        passwordUtil = new PasswordUtil();
    }

    @Test
    @DisplayName("正常：加密后与原密码匹配")
    void matches_correctPassword() {
        String raw = "doctor123";
        String encoded = passwordUtil.encode(raw);
        assertNotEquals(raw, encoded);
        assertTrue(passwordUtil.matches(raw, encoded));
    }

    @Test
    @DisplayName("异常：错误密码不匹配")
    void matches_wrongPassword() {
        String encoded = passwordUtil.encode("doctor123");
        assertFalse(passwordUtil.matches("wrong", encoded));
    }

    @Test
    @DisplayName("边界：空字符串可加密与校验")
    void matches_emptyPassword() {
        String encoded = passwordUtil.encode("");
        assertTrue(passwordUtil.matches("", encoded));
        assertFalse(passwordUtil.matches(" ", encoded));
    }
}
