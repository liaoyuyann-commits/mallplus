package com.macro.mall.common.cache;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机过期工具：在基准 TTL 上增加 10% 的随机抖动，降低缓存雪崩概率。
 */
public final class ExpireTimeUtil {

    private ExpireTimeUtil() {
    }

    public static long getExpireTime(long baseExpireTime) {
        if (baseExpireTime <= 0) {
            return 0;
        }
        double jitterRatio = 0.1D;
        long jitter = (long) (baseExpireTime * jitterRatio);
        long randomDelta = ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
        return Math.max(1, baseExpireTime + randomDelta);
    }
}
