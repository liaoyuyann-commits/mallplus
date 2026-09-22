package com.macro.mall.common.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpireTimeUtilTest {

    @Test
    void shouldReturnValidExpireTimeWithJitter() {
        long expireTime = ExpireTimeUtil.getExpireTime(600);
        assertTrue(expireTime >= 540 && expireTime <= 660, "Expire time should include a small random jitter around the base value");
    }
}
