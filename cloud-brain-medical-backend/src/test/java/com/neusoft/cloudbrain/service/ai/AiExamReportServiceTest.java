package com.neusoft.cloudbrain.service.ai;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AiExamReportServiceTest {

    private final AiExamReportService service = new AiExamReportService();

    @RepeatedTest(50)
    void generateCardiacEnzymeReport() {
        assertDoesNotThrow(() -> {
            var draft = service.generateExamReport("心肌酶", "生化", "胸痛待查");
            assertNotNull(draft.getReportText());
            assertNotNull(draft.getConclusion());
        });
    }

    @Test
    void generateAllCommonTypes() {
        String[] types = {
                "心电图", "心肌酶", "血常规", "尿常规", "肝功能", "肾功能", "血糖", "血脂",
                "凝血功能", "甲状腺", "肿瘤标志物", "电解质", "C反应蛋白", "胸片", "头部CT",
                "胸部CT", "腹部CT", "MRI", "腹部超声", "心脏超声", "胃镜", "肠镜", "肺功能",
                "骨密度", "病理", "血压", "眼压", "听力", "血气分析", "常规检查"
        };
        for (String type : types) {
            assertDoesNotThrow(() -> service.generateExamReport(type, "测试部位", "测试指征"),
                    "Failed for type: " + type);
        }
    }
}
