package com.neusoft.cloudbrain.dto.request.prescription;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReviewPrescriptionRequest {

    @NotNull
    private Long patientId;

    @NotEmpty
    private List<PrescriptionItemRequest> items;

    /** 初步诊断，用于知识图谱禁忌检索 */
    private String diagnosis;
}
