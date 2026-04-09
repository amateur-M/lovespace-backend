package com.meng.lovespace.user.exception;

/**
 * 情书生成等业务异常。
 */
public class LoveLetterBusinessException extends RuntimeException {

    private final int code;

    public LoveLetterBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
