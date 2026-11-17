package com.neusoft.cloudbrain.dto.request.equipment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class SaveMedicalEquipmentRequest {

    @NotBlank(message = "设备编号不能为空")
    private String equipmentCode;

    @NotBlank(message = "设备名称不能为空")
    private String equipmentName;

    @NotBlank(message = "设备分类不能为空")
    private String category;

    private Long departmentId;
    private String model;
    private String manufacturer;
    private String location;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate purchaseDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate lastMaintenanceDate;

    private String remark;
}
