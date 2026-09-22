package com.macro.mall.common.cache;

import com.macro.mall.common.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁：SETNX + UUID value，释放时校验 owner 防误删，避免破坏其他线程的锁。
 */
@Component
public class CacheLockUtil {

    private final Map<String, String> lockOwnerMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisService redisService;

    public boolean lock(String key, long timeout, TimeUnit unit) {
        String value = UUID.randomUUID().toString();
        boolean success = Boolean.TRUE.equals(redisService.setIfAbsent(key, value, unit.toSeconds(timeout)));
        if (success) {
            lockOwnerMap.put(key, value);
        }
        return success;
    }

    public boolean unlock(String key) {
        String ownerValue = lockOwnerMap.remove(key);
        if (ownerValue == null) {
            return false;
        }
        Object value = redisService.get(key);
        if (value == null) {
            return true;
        }
        if (!ownerValue.equals(String.valueOf(value))) {
            return false;
        }
        return Boolean.TRUE.equals(redisService.del(key));
    }
}
