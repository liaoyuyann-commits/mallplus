package com.macro.mall.portal.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.common.rocketmq.RocketMqConstant;
import com.macro.mall.portal.domain.CartMergeMessage;
import com.macro.mall.portal.service.OmsCartItemService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 购物车合并消费者。幂等判断由 CartRedisService 在业务 Service 内完成。
 */
@Component
@RocketMQMessageListener(
        topic = RocketMqConstant.TOPIC_CACHE,
        selectorExpression = RocketMqConstant.TAG_CART_MERGE,
        consumerGroup = RocketMqConstant.GROUP_PORTAL_CART)
public class CartMergeConsumer implements RocketMQListener<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CartMergeConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OmsCartItemService cartItemService;

    @Override
    public void onMessage(String message) {
        try {
            CartMergeMessage mergeMessage = objectMapper.readValue(
                    message.getBytes(StandardCharsets.UTF_8), CartMergeMessage.class);
            cartItemService.merge(mergeMessage.getMemberId(), mergeMessage.getRequest());
        } catch (Exception exception) {
            LOGGER.error("failed to consume cart merge message", exception);
            throw new IllegalStateException("cart merge message will be retried", exception);
        }
    }
}
