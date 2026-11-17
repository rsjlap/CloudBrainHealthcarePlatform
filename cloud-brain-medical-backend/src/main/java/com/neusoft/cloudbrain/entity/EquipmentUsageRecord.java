package com.neusoft.cloudbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("equipment_usage_record")
public class EquipmentUsageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("equipment_id")
    private Long equipmentId;

    @TableField("usage_type")
    private String usageType;

    @TableField("operator_name")
    private String operatorName;

    @TableField("patient_info")
    private String patientInfo;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    private String remark;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
