package com.neusoft.cloudbrain.service.kg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("KgDiseaseResolver 疾病名规范化")
class KgDiseaseResolverTest {

    private KgDiseaseResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new KgDiseaseResolver();
    }

    @Test
    @DisplayName("正常：取第一个有效候�?)
    void resolve_firstValidCandidate() {
        assertEquals("高血�?, resolver.resolve("", "  高血�? ", "糖尿�?));
    }

    @Test
    @DisplayName("正常：去除前缀噪声")
    void resolve_stripsLeadingNoise() {
        assertEquals("冠心�?, resolver.resolve("初步诊断：冠心病"));
        assertEquals("肺炎", resolver.resolve("疑似 肺炎"));
    }

    @Test
    @DisplayName("正常：截断问号与逗号后内�?)
    void resolve_truncatesAtPunctuation() {
        assertEquals("头痛", resolver.resolve("头痛？待�?));
        assertEquals("感冒", resolver.resolve("感冒，伴咳嗽"));
    }

    @Test
    @DisplayName("正常：超长文本截断至30字符")
    void resolve_truncatesLongText() {
        String longName = "abcdefghijklmnopqrstuvwxyz1234567890";
        assertEquals(30, resolver.resolve(longName).length());
    }

    @Test
    @DisplayName("边界：无有效候选返回空�?)
    void resolve_emptyWhenNoCandidate() {
        assertEquals("", resolver.resolve("", null, "   "));
    }
}
