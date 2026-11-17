package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.util.DesensitizeUtil;
import com.neusoft.cloudbrain.common.util.JwtUtil;
import com.neusoft.cloudbrain.common.util.PasswordUtil;
import com.neusoft.cloudbrain.config.JwtConfig;
import com.neusoft.cloudbrain.dto.request.auth.LoginRequest;
import com.neusoft.cloudbrain.dto.response.auth.LoginResponse;
import com.neusoft.cloudbrain.dto.response.auth.UserInfoVO;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        if (!passwordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名或密码错误");
        }
        return buildLoginResponse(user);
    }

    public UserInfoVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return UserInfoVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(DesensitizeUtil.maskPhone(user.getPhone()))
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpirationMs())
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .realName(user.getRealName())
                .build();
    }
}
