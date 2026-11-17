package com.neusoft.cloudbrain.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("DesensitizeUtil 脱敏工具")
class DesensitizeUtilTest {

    @Test
    @DisplayName("正常：手机号脱敏")
    void maskPhone_normal() {
        assertEquals("138****5678", DesensitizeUtil.maskPhone("13812345678"));
    }

    @Test
    @DisplayName("边界：null 或过短号码原样返回")
    void maskPhone_boundary() {
        assertNull(DesensitizeUtil.maskPhone(null));
        assertEquals("123456", DesensitizeUtil.maskPhone("123456"));
    }

    @Test
    @DisplayName("正常：身份证脱敏")
    void maskIdCard_normal() {
        String masked = DesensitizeUtil.maskIdCard("110101199001011234");
        assertEquals("1101**********1234", masked);
    }

    @Test
    @DisplayName("边界：身份证过短原样返回")
    void maskIdCard_boundary() {
        assertNull(DesensitizeUtil.maskIdCard(null));
        assertEquals("1234567", DesensitizeUtil.maskIdCard("1234567"));
    }
}
