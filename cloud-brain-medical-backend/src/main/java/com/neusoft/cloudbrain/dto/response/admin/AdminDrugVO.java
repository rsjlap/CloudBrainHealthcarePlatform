package com.neusoft.cloudbrain.dto.response.admin;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminDrugVO {

    private Long id;
    private String drugCode;
    private String drugName;
    private String specification;
    private String unit;
    private BigDecimal unitPrice;
    private String manufacturer;
    private String contraindication;
    private Integer status;
    private Integer stockQuantity;
}
