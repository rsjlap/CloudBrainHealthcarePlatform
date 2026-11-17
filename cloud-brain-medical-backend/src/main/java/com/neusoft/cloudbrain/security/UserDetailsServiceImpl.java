package com.neusoft.cloudbrain.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.entity.User;
import com.neusoft.cloudbrain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        return new SecurityUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus() == 1
        );
    }
}
