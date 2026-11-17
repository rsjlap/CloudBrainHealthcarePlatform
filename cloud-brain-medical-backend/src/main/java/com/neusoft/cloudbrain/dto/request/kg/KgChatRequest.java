package com.neusoft.cloudbrain.dto.request.kg;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KgChatRequest {

    @NotBlank
    private String question;
}
