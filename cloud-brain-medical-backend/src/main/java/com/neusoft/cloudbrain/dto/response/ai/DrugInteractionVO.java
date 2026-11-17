package com.neusoft.cloudbrain.dto.response.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DrugInteractionVO {

    private String drug1;
    private String drug2;
    private String description;
    private String severity;
}
