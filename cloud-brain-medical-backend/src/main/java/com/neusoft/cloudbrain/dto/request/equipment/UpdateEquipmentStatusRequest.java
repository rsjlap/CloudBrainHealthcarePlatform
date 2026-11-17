package com.neusoft.cloudbrain.dto.request.equipment;

import com.neusoft.cloudbrain.common.enums.EquipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateEquipmentStatusRequest {

    @NotNull(message = "请指定设备状态")
    private EquipmentStatus status;
}
