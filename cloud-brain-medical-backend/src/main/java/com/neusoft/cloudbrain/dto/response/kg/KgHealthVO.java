package com.neusoft.cloudbrain.dto.response.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgHealthVO {

    private boolean enabled;
    private boolean healthy;
    private String baseUrl;
    private String apiMode;
    /** 连接失败时的具体原因，便于排查 */
    private String message;
}
