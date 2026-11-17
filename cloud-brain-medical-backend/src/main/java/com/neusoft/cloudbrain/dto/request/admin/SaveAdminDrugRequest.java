package com.neusoft.cloudbrain.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaveAdminDrugRequest {

    @NotBlank(message = "药品编码不能为空")
    private String drugCode;

    @NotBlank(message = "药品名称不能为空")
    private String drugName;

    private String specification;
    private String unit;
    private BigDecimal unitPrice;
    private String manufacturer;
    private String contraindication;
    private Integer status;
    private Integer stockQuantity;
}
