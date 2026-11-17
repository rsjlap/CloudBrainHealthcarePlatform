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
@TableName("exam_report")
public class ExamReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("report_text")
    private String reportText;

    private String conclusion;

    @TableField("image_urls")
    private String imageUrls;

    @TableField("is_ai_generated")
    private Integer isAiGenerated = 0;

    @TableField("inspector_id")
    private Long inspectorId;

    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
