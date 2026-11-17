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
@TableName("prescription_item")
public class PrescriptionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("prescription_id")
    private Long prescriptionId;

    @TableField("drug_id")
    private Long drugId;

    @TableField("drug_name")
    private String drugName;

    @TableField("drug_code")
    private String drugCode;

    private BigDecimal dosage;

    private String unit;

    private String frequency;

    @TableField("duration_days")
    private Integer durationDays;

    @TableField("total_amount")
    private BigDecimal totalAmount;
}
