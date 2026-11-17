package com.neusoft.cloudbrain.common.util;

import com.neusoft.cloudbrain.common.result.ResultCode;
import com.neusoft.cloudbrain.common.exception.BusinessException;
import com.neusoft.cloudbrain.security.SecurityUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    public static SecurityUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUserDetails userDetails)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userDetails;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
}
