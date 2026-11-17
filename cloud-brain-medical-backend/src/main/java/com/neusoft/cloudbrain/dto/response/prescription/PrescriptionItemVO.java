package com.neusoft.cloudbrain.dto.response.prescription;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PrescriptionItemVO {

    private Long id;
    private Long drugId;
    private String drugName;
    private String drugCode;
    private BigDecimal dosage;
    private String unit;
    private String frequency;
    private Integer durationDays;
    private BigDecimal totalAmount;
}
