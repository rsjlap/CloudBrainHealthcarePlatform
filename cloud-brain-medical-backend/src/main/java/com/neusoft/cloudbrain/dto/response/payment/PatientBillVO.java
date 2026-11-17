package com.neusoft.cloudbrain.dto.response.payment;

import com.neusoft.cloudbrain.common.enums.BillType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientBillVO {

    private BillType billType;
    private Long relatedId;
    private String title;
    private BigDecimal amount;
    private boolean paid;
    private LocalDateTime createdAt;
}
