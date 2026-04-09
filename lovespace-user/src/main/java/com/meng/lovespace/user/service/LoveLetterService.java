package com.meng.lovespace.user.service;

/**
 * AI 情书生成：基于情侣信息、恋爱天数与共同回忆（或时间轴摘要）。
 */
public interface LoveLetterService {

    /**
     * 生成情书正文（通义千问）。
     *
     * @param userId 当前用户
     * @param coupleId 情侣绑定 ID
     * @param style romantic / humorous / sincere
     * @param length short / medium / long
     * @param memories 可选；为空时从可见恋爱记录摘要拼接
     * @return 情书正文
     */
    String generate(String userId, String coupleId, String style, String length, String memories);
}
