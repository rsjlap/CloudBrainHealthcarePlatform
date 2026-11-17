package com.neusoft.cloudbrain.dto.request.prescription;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PrescriptionItemRequest {

    /** 药品目录 id（开方时优先使用） */
    private Long drugId;

    @NotBlank
    private String drugName;

    private String drugCode;
    private BigDecimal dosage;
    private String unit;
    private String frequency;
    private Integer durationDays;
    private BigDecimal totalAmount;

    /** AI 推荐时标记是否匹配药品目录（不入库） */
    private Boolean catalogMatched;
}
