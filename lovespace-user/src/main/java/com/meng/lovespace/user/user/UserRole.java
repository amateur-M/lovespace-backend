package com.meng.lovespace.user.user;

/**
 * 用户角色枚举，映射 {@code users.role}。
 */
public enum UserRole {
    /** 普通用户 */
    USER(0),
    /** 平台管理员 */
    ADMIN(1);

    private final int code;

    UserRole(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static UserRole fromCode(Integer code) {
        if (code == null) {
            return USER;
        }
        for (UserRole r : values()) {
            if (r.code == code) {
                return r;
            }
        }
        return USER;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
