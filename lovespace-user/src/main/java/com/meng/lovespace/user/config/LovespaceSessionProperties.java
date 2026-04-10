package com.meng.lovespace.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 会话相关配置：可选的 Redis 分布式 Session（多实例共享登录态）。
 *
 * <p>启用时需同时设置 {@code spring.session.store-type=redis} 并保证 Redis 可用，参见 {@code application.yml} 注释。
 */
@ConfigurationProperties(prefix = "lovespace.session")
public class LovespaceSessionProperties {

    private final Distributed distributed = new Distributed();

    public Distributed getDistributed() {
        return distributed;
    }

    public static class Distributed {
        /**
         * 是否启用 Redis 分布式 Session。默认关闭；为 true 时登录会写入服务端 Session，刷新页可仅凭 Cookie 保持登录（与 JWT 并存）。
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
