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
 * 检查项目目录（需使用医疗设备）。
 */
@Getter
@Setter
@TableName("exam_item")
public class ExamItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("item_code")
    private String itemCode;

    @TableField("item_name")
    private String itemName;

    @TableField("body_part")
    private String bodyPart;

    private String description;

    @TableField("department_id")
    private Long departmentId;

    @TableField("equipment_type")
    private String equipmentType;

    private BigDecimal fee;

    private CatalogItemStatus status = CatalogItemStatus.ENABLED;
}
