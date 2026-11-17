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
@TableName("doctor")
public class Doctor {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("department_id")
    private Long departmentId;

    private String title;

    private String specialty;

    @TableField("registration_fee")
    private BigDecimal registrationFee;

    private String introduction;

    @TableField("is_available")
    private Integer isAvailable = 1;
}
