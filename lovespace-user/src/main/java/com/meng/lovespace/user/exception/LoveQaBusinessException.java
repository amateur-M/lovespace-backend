package com.meng.lovespace.user.exception;

/** 恋爱问答业务异常（查看历史等场景）。 */
public class LoveQaBusinessException extends RuntimeException {

    private final int code;

    public LoveQaBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
