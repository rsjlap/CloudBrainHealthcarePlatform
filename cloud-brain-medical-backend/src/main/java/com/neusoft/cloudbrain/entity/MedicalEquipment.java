package com.neusoft.cloudbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.neusoft.cloudbrain.common.enums.EquipmentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("medical_equipment")
public class MedicalEquipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("equipment_code")
    private String equipmentCode;

    @TableField("equipment_name")
    private String equipmentName;

    private String category;

    @TableField("department_id")
    private Long departmentId;

    private String model;

    private String manufacturer;

    private String location;

    private EquipmentStatus status = EquipmentStatus.ACTIVE;

    @TableField("purchase_date")
    private LocalDate purchaseDate;

    @TableField("last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    private String remark;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
