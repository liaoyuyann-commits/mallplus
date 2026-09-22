package com.macro.mall.portal.component;

import com.macro.mall.common.cache.CacheKeyConstant;
import com.macro.mall.common.cache.BloomFilterService;
import com.macro.mall.common.rocketmq.RocketMqConstant;
import com.macro.mall.common.cache.LocalCacheManager;
import com.macro.mall.common.service.RedisService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 商品缓存失效消费者。为什么存在：商品更新发生在 admin，portal 进程内的 Caffeine 无法被直接感知。
 */
@Component
@RocketMQMessageListener(
        topic = RocketMqConstant.TOPIC_CACHE,
        selectorExpression = RocketMqConstant.TAG_PRODUCT_EVICT,
        consumerGroup = RocketMqConstant.GROUP_PORTAL_CACHE)
public class CacheEvictReceiver implements RocketMQListener<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheEvictReceiver.class);

    @Autowired
    private LocalCacheManager localCacheManager;
    @Autowired
    private RedisService redisService;
    @Autowired
    private BloomFilterService bloomFilterService;

    @Override
    public void onMessage(String productIdMessage) {
        String productId = productIdMessage.trim();
        String detailKey = CacheKeyConstant.PRODUCT_DETAIL.replace("{id}", productId);
        String emptyKey = CacheKeyConstant.PRODUCT_EMPTY.replace("{id}", productId);
        bloomFilterService.add(CacheKeyConstant.PRODUCT_BLOOM + productId);
        localCacheManager.remove(detailKey);
        redisService.del(detailKey);
        redisService.del(emptyKey);
        localCacheManager.remove(CacheKeyConstant.HOME_CONTENT);
        redisService.del(CacheKeyConstant.HOME_CONTENT);
        LOGGER.info("evicted product cache, productId:{}", productId);
    }
}