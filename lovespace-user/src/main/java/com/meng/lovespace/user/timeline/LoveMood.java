package com.meng.lovespace.user.timeline;

import java.util.Set;

/**
 * 允许的心情标签（与产品约定一致）。
 */
public final class LoveMood {

    public static final Set<String> ALLOWED =
            Set.of("happy", "sad", "excited", "calm", "loved", "missed");

    private LoveMood() {}

    public static boolean isAllowed(String mood) {
        return mood != null && ALLOWED.contains(mood);
    }
}
