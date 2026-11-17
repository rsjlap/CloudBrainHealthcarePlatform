package com.neusoft.cloudbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.neusoft.cloudbrain.common.enums.PrescriptionStatus;
import com.neusoft.cloudbrain.common.enums.RiskLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("prescription")
public class Prescription {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("registration_id")
    private Long registrationId;

    @TableField("doctor_id")
    private Long doctorId;

    private PrescriptionStatus status = PrescriptionStatus.PENDING_DISPENSE;

    @TableField("ai_review_result")
    private String aiReviewResult;

    @TableField("risk_level")
    private RiskLevel riskLevel;

    @TableField("dispensed_by")
    private Long dispensedBy;

    @TableField("dispensed_at")
    private LocalDateTime dispensedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField("paid_at")
    private LocalDateTime paidAt;
}
