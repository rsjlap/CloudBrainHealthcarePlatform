package com.neusoft.cloudbrain.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AiJsonResponseUtil JSON 提取")
class AiJsonResponseUtilTest {

    @Test
    @DisplayName("边界：null 与空白返回 {}")
    void extractJson_nullOrBlank() {
        assertEquals("{}", AiJsonResponseUtil.extractJson(null));
        assertEquals("{}", AiJsonResponseUtil.extractJson("   "));
    }

    @Test
    @DisplayName("正常：纯 JSON 对象")
    void extractJson_plainObject() {
        String raw = "  {\"chiefComplaint\":\"头痛\"}  ";
        assertEquals("{\"chiefComplaint\":\"头痛\"}", AiJsonResponseUtil.extractJson(raw));
    }

    @Test
    @DisplayName("正常：Markdown 代码块包裹的 JSON")
    void extractJson_markdownCodeBlock() {
        String raw = "```json\n{\"diagnosis\":\"感冒\"}\n```";
        assertEquals("{\"diagnosis\":\"感冒\"}", AiJsonResponseUtil.extractJson(raw));
    }

    @Test
    @DisplayName("正常：前后有多余文字时提取 JSON 片段")
    void extractJson_withPrefixText() {
        String raw = "以下是病历：{\"diagnosis\":\"胃炎\"}，请核对。";
        String json = AiJsonResponseUtil.extractJson(raw);
        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("胃炎"));
    }

    @Test
    @DisplayName("边界：无 JSON 结构时返回原文 trim")
    void extractJson_noJsonStructure() {
        assertEquals("纯文本", AiJsonResponseUtil.extractJson("纯文本"));
    }
}
