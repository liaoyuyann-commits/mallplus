package com.macro.mall.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存封装，负责短期热点数据的命中和失效。
 */
@Component
public class LocalCacheManager {

    private final Cache<String, Object> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(10000)
            .build();

    public Object get(String key) {
        return cache.getIfPresent(key);
    }

    public void put(String key, Object value) {
        cache.put(key, value);
    }

    public void put(String key, Object value, long duration, TimeUnit unit) {
        cache.asMap().put(key, value);
    }

    public void remove(String key) {
        cache.invalidate(key);
    }

    public void removeAll() {
        cache.invalidateAll();
    }
}
