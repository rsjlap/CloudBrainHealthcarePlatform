package com.neusoft.cloudbrain.dto.request.exam;

import com.neusoft.cloudbrain.common.enums.ExamOrderType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateExamOrderRequest {

    @NotNull(message = "挂号ID不能为空")
    private Long registrationId;

    /** 申请类型：LAB=检验 EXAM=检查 */
    private ExamOrderType orderType;

    /** 检验项目 id（orderType=LAB 时必填） */
    private Long labItemId;

    /** 检查项目 id（orderType=EXAM 时必填） */
    private Long examItemId;

    /** 兼容旧版手填（无目录 id 时使用） */
    private String examType;

    private String bodyPart;

    private String clinicalIndication;
}
