package com.neusoft.cloudbrain.dto.request.admin;

import lombok.Data;

@Data
public class UpdateAdminStaffRequest {

    private String realName;
    private String phone;
    private String password;
    private Integer status;
}
