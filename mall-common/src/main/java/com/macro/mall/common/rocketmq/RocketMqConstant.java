package com.macro.mall.common.rocketmq;

/**
 * RocketMQ 主题、标签与消费组常量统一管理，避免业务代码里硬编码。
 */
public interface RocketMqConstant {

    String TOPIC_CACHE = "MALL_CACHE";
    String TAG_PRODUCT_EVICT = "PRODUCT_EVICT";
    String TAG_CART_MERGE = "CART_MERGE";

    String GROUP_PORTAL_CACHE = "mall-portal-cache-group";
    String GROUP_PORTAL_CART = "mall-portal-cart-group";
    String GROUP_ADMIN_CACHE = "mall-admin-cache-group";
}
