package com.neusoft.cloudbrain.dto.request.registration;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRegistrationRequest {

    @NotNull(message = "医生ID不能为空")
    private Long doctorId;

    @NotNull(message = "排班ID不能为空")
    private Long scheduleId;

    private String chiefComplaint;
}
