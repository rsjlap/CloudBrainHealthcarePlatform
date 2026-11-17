package com.neusoft.cloudbrain.dto.response.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminAiChatResponseVO {

    private String answer;
    private String queryType;
    private List<String> sources;
    private boolean aiAvailable;
}
