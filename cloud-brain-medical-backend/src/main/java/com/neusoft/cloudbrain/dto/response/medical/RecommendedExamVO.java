package com.neusoft.cloudbrain.dto.response.medical;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.neusoft.cloudbrain.common.enums.ExamOrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendedExamVO {

    private String examType;
    private String bodyPart;
    private String clinicalIndication;
    private Integer priority;
    /** 目录类型：LAB=检验 EXAM=检查 */
    private ExamOrderType orderType;
    /** 匹配的目录项 id */
    private Long catalogItemId;
    private String itemCode;
    /** 是否已在 lab_item / exam_item 中匹配 */
    private Boolean catalogMatched;
}
