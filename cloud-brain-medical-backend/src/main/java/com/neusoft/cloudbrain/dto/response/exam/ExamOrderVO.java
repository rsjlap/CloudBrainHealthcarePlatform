package com.neusoft.cloudbrain.dto.response.exam;

import com.neusoft.cloudbrain.common.enums.ExamOrderStatus;
import com.neusoft.cloudbrain.common.enums.ExamOrderType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ExamOrderVO {

    private Long id;
    private Long registrationId;
    private String patientName;
    private ExamOrderType orderType;
    private Long labItemId;
    private Long examItemId;
    private String examType;
    private String bodyPart;
    private String clinicalIndication;
    private BigDecimal fee;
    private ExamOrderStatus status;
    private String orderedByName;
    private Boolean paid;
    private LocalDateTime createdAt;
}
