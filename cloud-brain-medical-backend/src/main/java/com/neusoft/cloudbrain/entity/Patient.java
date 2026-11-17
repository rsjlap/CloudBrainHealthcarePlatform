package com.neusoft.cloudbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@TableName("patient")
public class Patient {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("id_card")
    private String idCard;

    @TableField("birth_date")
    private LocalDate birthDate;

    private Integer gender;

    @TableField("blood_type")
    private String bloodType;

    @TableField("allergy_history")
    private String allergyHistory;

    @TableField("medical_history")
    private String medicalHistory;

    @TableField("emergency_contact")
    private String emergencyContact;

    @TableField("emergency_phone")
    private String emergencyPhone;
}
