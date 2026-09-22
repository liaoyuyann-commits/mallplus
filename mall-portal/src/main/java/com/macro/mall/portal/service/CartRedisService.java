package com.macro.mall.portal.service;

import com.macro.mall.portal.domain.CartRedisItem;

import java.util.List;
import java.util.Map;

/**
 * 已登录购物车 Redis Hash 服务。
 * 为什么存在：把 Redis key、Hash 读写和幂等标记集中管理，避免业务 Service 直接拼接 Redis 细节。
 */
public interface CartRedisService {

    boolean markMergeRequest(Long memberId, String mergeRequestId);

    void clearMergeRequest(Long memberId, String mergeRequestId);

    Map<String, CartRedisItem> readItems(Long memberId);

    void writeItems(Long memberId, Map<String, CartRedisItem> items);

    List<CartRedisItem> listItems(Long memberId);

    /**
     * 覆盖某个 field 的数量；field 不存在时返回 false。
     */
    boolean updateQuantity(Long memberId, String field, Integer quantity);

    /**
     * 批量删除 field；返回实际删除的条目数。
     */
    Long removeItems(Long memberId, List<String> fields);

    boolean hasItems(Long memberId);

    void clearItems(Long memberId);
}
