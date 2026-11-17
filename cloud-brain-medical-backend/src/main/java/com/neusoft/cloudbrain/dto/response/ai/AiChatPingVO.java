package com.neusoft.cloudbrain.dto.response.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatPingVO {

    private boolean available;
    private String model;
    private String prompt;
    private String reply;
    private String error;
}
