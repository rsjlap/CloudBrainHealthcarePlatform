package com.neusoft.cloudbrain.dto.response.catalog;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExamItemVO {

    private Long id;
    private String itemCode;
    private String itemName;
    private String bodyPart;
    private String description;
    private Long departmentId;
    private String departmentName;
    private String equipmentType;
    private BigDecimal fee;
}
