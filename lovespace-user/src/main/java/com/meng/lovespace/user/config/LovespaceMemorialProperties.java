package com.meng.lovespace.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 纪念日模块：Redis 缓存与「未来窗口」天数。
 */
@Data
@ConfigurationProperties(prefix = "lovespace.memorial")
public class LovespaceMemorialProperties {

    /** Redis 中 next / upcoming 条目的 TTL（秒）。 */
    private int cacheTtlSeconds = 3600;

    /** 「近期」列表包含从今天起共多少天（含今天）。默认 7。 */
    private int upcomingWindowDays = 7;

    /** 倒计时使用的时区 ID，默认 Asia/Shanghai。 */
    private String zoneId = "Asia/Shanghai";
}
