package com.neusoft.cloudbrain.dto.request.medical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateMedicalRecordRequest {

    @NotNull
    private Long registrationId;

    @NotBlank
    private String dialogText;
}
