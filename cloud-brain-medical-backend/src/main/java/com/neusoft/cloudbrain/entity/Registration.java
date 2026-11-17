package com.neusoft.cloudbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("registration")
public class Registration {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("reg_no")
    private String regNo;

    @TableField("patient_id")
    private Long patientId;

    @TableField("doctor_id")
    private Long doctorId;

    @TableField("department_id")
    private Long departmentId;

    @TableField("schedule_id")
    private Long scheduleId;

    private RegistrationStatus status = RegistrationStatus.PENDING;

    @TableField("queue_no")
    private Integer queueNo;

    @TableField("visit_type")
    private Integer visitType;

    @TableField("chief_complaint")
    private String chiefComplaint;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField("paid_at")
    private LocalDateTime paidAt;
}
