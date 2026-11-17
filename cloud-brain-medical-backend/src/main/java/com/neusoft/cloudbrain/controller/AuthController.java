package com.neusoft.cloudbrain.controller;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.common.util.SecurityContextUtil;
import com.neusoft.cloudbrain.dto.request.auth.LoginRequest;
import com.neusoft.cloudbrain.dto.response.auth.LoginResponse;
import com.neusoft.cloudbrain.dto.response.auth.UserInfoVO;
import com.neusoft.cloudbrain.config.SwaggerConfig;
import com.neusoft.cloudbrain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证模块")
@RestController
@RequestMapping(ApiConstants.BASE_URL + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "统一登录")
    @SecurityRequirements
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "获取当前用户信息")
    @SecurityRequirement(name = SwaggerConfig.AUTH_SCHEME)
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(authService.getCurrentUser(SecurityContextUtil.getCurrentUserId()));
    }

    @Operation(summary = "登出")
    @SecurityRequirement(name = SwaggerConfig.AUTH_SCHEME)
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
