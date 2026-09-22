package com.macro.mall.portal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.common.rocketmq.RocketMqConstant;
import com.macro.mall.portal.domain.CartMergeMessage;
import com.macro.mall.portal.domain.CartMergeRequest;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 购物车合并消息生产者。
 * 为什么用 RocketMQTemplate：生产端与消费端统一走 rocketmq-spring-boot-starter，
 * 避免手动 new DefaultMQProducer 与 starter 的 RocketMQTemplate 重复启动同一 producer。
 */
@Service
public class CartMergeProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    public void send(Long memberId, CartMergeRequest request) {
        try {
            CartMergeMessage messageBody = new CartMergeMessage();
            messageBody.setMemberId(memberId);
            messageBody.setRequest(request);
            String destination = RocketMqConstant.TOPIC_CACHE + ":" + RocketMqConstant.TAG_CART_MERGE;
            rocketMQTemplate.convertAndSend(destination, objectMapper.writeValueAsString(messageBody));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to send cart merge message", exception);
        }
    }
}
