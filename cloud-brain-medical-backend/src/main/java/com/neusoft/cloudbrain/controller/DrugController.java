package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.config.SwaggerConfig;
import com.neusoft.cloudbrain.dto.response.drug.DrugVO;
import com.neusoft.cloudbrain.service.DrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "药品模块")
@SecurityRequirement(name = SwaggerConfig.AUTH_SCHEME)
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugService drugService;

    @Operation(summary = "药品列表")
    @GetMapping
    public Result<List<DrugVO>> list() {
        return Result.success(drugService.listActive());
    }
}
