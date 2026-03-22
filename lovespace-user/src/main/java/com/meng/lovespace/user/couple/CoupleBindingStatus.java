package com.meng.lovespace.user.couple;

/**
 * {@code couple_binding.status} 取值。
 *
 * <p>0 为邀请流程扩展状态；1–3 与表设计一致。
 */
public final class CoupleBindingStatus {

    /** 邀请已发出，等待对方接受 */
    public static final int PENDING = 0;
    /** 交往中 */
    public static final int ACTIVE = 1;
    /** 冻结 */
    public static final int FROZEN = 2;
    /** 已解除 */
    public static final int SEPARATED = 3;

    private CoupleBindingStatus() {}
}
