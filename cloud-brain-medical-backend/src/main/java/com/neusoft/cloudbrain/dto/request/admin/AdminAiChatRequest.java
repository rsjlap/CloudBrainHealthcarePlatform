package com.neusoft.cloudbrain.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminAiChatRequest {

    @NotBlank(message = "请输入问题")
    private String question;
}
