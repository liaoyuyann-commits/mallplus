package com.macro.mall.portal.service.impl;


import com.macro.mall.common.cache.CacheKeyConstant;
import com.macro.mall.common.service.RedisService;
import com.macro.mall.portal.domain.CartRedisItem;
import com.macro.mall.portal.service.CartRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 已登录购物车 Redis Hash 实现。
 */
@Service
public class CartRedisServiceImpl implements CartRedisService {

    private static final long IDEMPOTENT_SECONDS = 24 * 60 * 60L;
    private static final long CART_EXPIRE_SECONDS = 30 * 24 * 60 * 60L;

    @Autowired
    private RedisService redisService;

    @Override
    public boolean markMergeRequest(Long memberId, String mergeRequestId) {
        return Boolean.TRUE.equals(redisService.setIfAbsent(
                mergeRequestKey(memberId, mergeRequestId), true, IDEMPOTENT_SECONDS));
    }

    @Override
    public void clearMergeRequest(Long memberId, String mergeRequestId) {
        redisService.del(mergeRequestKey(memberId, mergeRequestId));
    }

    @Override
    public Map<String, CartRedisItem> readItems(Long memberId) {
        Map<String, CartRedisItem> items = new HashMap<>();
        for (Map.Entry<Object, Object> entry : redisService.hGetAll(cartKey(memberId)).entrySet()) {
            items.put(String.valueOf(entry.getKey()), (CartRedisItem) entry.getValue());
        }
        return items;
    }

    @Override
    public void writeItems(Long memberId, Map<String, CartRedisItem> items) {
        redisService.hSetAll(cartKey(memberId), items);
        redisService.expire(cartKey(memberId), CART_EXPIRE_SECONDS);
    }

    @Override
    public List<CartRedisItem> listItems(Long memberId) {
        return new ArrayList<>(readItems(memberId).values());
    }

    @Override
    public boolean updateQuantity(Long memberId, String field, Integer quantity) {
        CartRedisItem item = (CartRedisItem) redisService.hGet(cartKey(memberId), field);
        if (item == null) {
            return false;
        }
        item.setQuantity(quantity);
        redisService.hSet(cartKey(memberId), field, item);
        return true;
    }

    @Override
    public Long removeItems(Long memberId, List<String> fields) {
        return redisService.hDel(cartKey(memberId), fields.toArray());
    }

    @Override
    public boolean hasItems(Long memberId) {
        return !readItems(memberId).isEmpty();
    }

    @Override
    public void clearItems(Long memberId) {
        redisService.del(cartKey(memberId));
    }

    private String cartKey(Long memberId) {
        return CacheKeyConstant.MEMBER_CART.replace("{memberId}", String.valueOf(memberId));
    }

    private String mergeRequestKey(Long memberId, String mergeRequestId) {
        return CacheKeyConstant.CART_MERGE_IDEMPOTENT
                .replace("{memberId}", String.valueOf(memberId))
                .replace("{mergeId}", mergeRequestId);
    }
}
