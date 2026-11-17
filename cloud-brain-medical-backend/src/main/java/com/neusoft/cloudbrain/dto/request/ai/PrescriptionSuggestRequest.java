package com.neusoft.cloudbrain.dto.request.ai;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrescriptionSuggestRequest {

    @NotNull
    private Long patientId;

    private String chiefComplaint;
    private String symptoms;
    private String diagnosis;
}
