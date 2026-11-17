package com.neusoft.cloudbrain.dto.request.doctor;

import lombok.Data;

@Data
public class UpdateDoctorProfileRequest {

    private String specialty;

    private String introduction;
}
