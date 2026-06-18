package com.meng.lovespace.user.admin;

import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.user.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 后台接口鉴权辅助。 */
public final class AdminAuthSupport {

    private AdminAuthSupport() {}

    public static JwtUserPrincipal requireAdmin(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof JwtUserPrincipal p)) {
            throw new ApiBusinessException(40300, "admin access required");
        }
        if (!UserRole.fromCode(p.role()).isAdmin()) {
            throw new ApiBusinessException(40300, "admin access required");
        }
        return p;
    }

    public static JwtUserPrincipal currentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return requireAdmin(auth);
    }
}
