package com.neusoft.cloudbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.neusoft.cloudbrain.common.enums.ExamOrderStatus;
import com.neusoft.cloudbrain.common.enums.ExamOrderType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("exam_order")
public class ExamOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("registration_id")
    private Long registrationId;

    @TableField("order_type")
    private ExamOrderType orderType = ExamOrderType.EXAM;

    @TableField("lab_item_id")
    private Long labItemId;

    @TableField("exam_item_id")
    private Long examItemId;

    @TableField("exam_type")
    private String examType;

    @TableField("body_part")
    private String bodyPart;

    @TableField("clinical_indication")
    private String clinicalIndication;

    private BigDecimal fee;

    private ExamOrderStatus status = ExamOrderStatus.PENDING;

    @TableField("ordered_by")
    private Long orderedBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField("paid_at")
    private LocalDateTime paidAt;
}
