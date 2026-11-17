package com.neusoft.cloudbrain.dto.response.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechTranscribeVO {

    private String text;

    private boolean speechAvailable;
}
