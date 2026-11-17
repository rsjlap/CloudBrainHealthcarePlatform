package com.neusoft.cloudbrain.dto.response.registration;

import com.neusoft.cloudbrain.common.enums.RegistrationStatus;
import com.neusoft.cloudbrain.common.enums.TimeSlot;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationVO {

    private Long id;
    private String regNo;
    private Long patientId;
    private String patientName;
    private String doctorName;
    private String deptName;
    private LocalDate workDate;
    private TimeSlot timeSlot;
    private RegistrationStatus status;
    private Integer queueNo;
    private String chiefComplaint;
    private LocalDateTime createdAt;
    private Boolean paid;
}
