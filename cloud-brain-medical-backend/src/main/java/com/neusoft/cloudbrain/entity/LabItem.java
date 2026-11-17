package com.neusoft.cloudbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.neusoft.cloudbrain.common.enums.CatalogItemStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 检验项目目录（无需设备）。
 */
@Getter
@Setter
@TableName("lab_item")
public class LabItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("item_code")
    private String itemCode;

    @TableField("item_name")
    private String itemName;

    private String category;

    private String description;

    @TableField("department_id")
    private Long departmentId;

    private BigDecimal fee;

    private CatalogItemStatus status = CatalogItemStatus.ENABLED;
}
