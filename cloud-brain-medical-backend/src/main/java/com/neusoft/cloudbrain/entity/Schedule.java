package com.neusoft.cloudbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.neusoft.cloudbrain.common.enums.TimeSlot;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@TableName("schedule")
public class Schedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("doctor_id")
    private Long doctorId;

    @TableField("work_date")
    private LocalDate workDate;

    @TableField("time_slot")
    private TimeSlot timeSlot;

    @TableField("max_patients")
    private Integer maxPatients;

    @TableField("registered_count")
    private Integer registeredCount = 0;

    @TableField("is_ai_generated")
    private Integer isAiGenerated = 0;
}
