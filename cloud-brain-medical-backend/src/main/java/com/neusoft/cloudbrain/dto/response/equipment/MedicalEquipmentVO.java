package com.neusoft.cloudbrain.dto.response.equipment;

import com.neusoft.cloudbrain.common.enums.EquipmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MedicalEquipmentVO {

    private Long id;
    private String equipmentCode;
    private String equipmentName;
    private String category;
    private Long departmentId;
    private String departmentName;
    private String model;
    private String manufacturer;
    private String location;
    private EquipmentStatus status;
    private String statusLabel;
    private LocalDate purchaseDate;
    private LocalDate lastMaintenanceDate;
    private String remark;
    private LocalDateTime updatedAt;
}
