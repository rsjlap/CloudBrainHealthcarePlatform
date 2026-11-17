package com.neusoft.cloudbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("drug")
public class Drug {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("drug_code")
    private String drugCode;

    @TableField("drug_name")
    private String drugName;

    private String specification;

    private String unit;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    private String manufacturer;

    private String contraindication;

    private Integer status = 1;

    @TableField("stock_quantity")
    private Integer stockQuantity = 100;
}
