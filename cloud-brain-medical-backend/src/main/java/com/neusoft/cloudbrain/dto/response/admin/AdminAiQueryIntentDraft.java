package com.neusoft.cloudbrain.dto.response.admin;

import lombok.Data;

@Data
public class AdminAiQueryIntentDraft {

    /** DRUG | REGISTRATION | EQUIPMENT | PATIENT | SCHEDULE | STAFF | DASHBOARD */
    private String intent;

    /** 药品名、设备名、医生名等 */
    private String keyword;

    /** 科室名，如「心内科」 */
    private String departmentName;

    /** TODAY | TOMORROW | WEEK | ALL */
    private String dateScope;

    /** 是否只关注仍可挂号的排班（有余号且医生可接诊） */
    private Boolean onlyAvailable;

    private Integer stockThreshold;
}
