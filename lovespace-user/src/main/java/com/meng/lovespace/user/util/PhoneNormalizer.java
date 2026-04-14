package com.meng.lovespace.user.util;

/**
 * 中国大陆手机号：去非数字字符，校验 11 位且以 1[3-9] 开头。
 */
public final class PhoneNormalizer {

    private PhoneNormalizer() {}

    /** 仅保留数字。 */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\D+", "");
    }

    /** 是否为合法中国大陆手机号（已归一化为纯数字）。 */
    public static boolean isValidCnMobile(String digits) {
        return digits != null && digits.matches("^1[3-9]\\d{9}$");
    }
}
