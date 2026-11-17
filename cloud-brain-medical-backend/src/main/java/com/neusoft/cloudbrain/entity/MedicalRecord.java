package com.neusoft.cloudbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.neusoft.cloudbrain.common.enums.MedicalRecordStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("medical_record")
public class MedicalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("registration_id")
    private Long registrationId;

    @TableField("patient_id")
    private Long patientId;

    @TableField("doctor_id")
    private Long doctorId;

    @TableField("chief_complaint")
    private String chiefComplaint;

    @TableField("present_illness")
    private String presentIllness;

    @TableField("past_history")
    private String pastHistory;

    @TableField("physical_exam")
    private String physicalExam;

    private String diagnosis;

    @TableField("treatment_plan")
    private String treatmentPlan;

    @TableField("dialog_text")
    private String dialogText;

    @TableField("ai_draft")
    private String aiDraft;

    @TableField("is_ai_generated")
    private Integer isAiGenerated = 0;

    private MedicalRecordStatus status = MedicalRecordStatus.DRAFT;

    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
