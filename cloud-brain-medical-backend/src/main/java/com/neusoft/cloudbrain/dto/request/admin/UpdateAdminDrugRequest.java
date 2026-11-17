package com.neusoft.cloudbrain.dto.request.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateAdminDrugRequest {

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
