package com.macro.mall.common.cache;

/**
 * 缓存Key统一管理，避免业务代码中散落硬编码。
 */
public interface CacheKeyConstant {

    String PRODUCT_DETAIL = "portal:product:detail:{id}";
    String PRODUCT_EMPTY = "portal:product:empty:{id}";
    String PRODUCT_BLOOM = "product:detail:";
    String PRODUCT_LIST = "portal:product:list:{categoryId}:{pageNum}:{pageSize}";
    String HOME_RECOMMEND = "portal:home:recommend:{type}";
    String HOME_CONTENT = "portal:home:content";
    String MEMBER_CART = "cart:member:{memberId}";
    String GUEST_CART = "cart:guest:{cookieValue}";
    String CART_MERGE_IDEMPOTENT = "cart:merge:request:{memberId}:{mergeId}";
    String LOCK_PRODUCT_DETAIL = "lock:portal:product:detail:{id}";
    String LOCK_HOME_CONTENT = "lock:portal:home:content";
    String LOCK_CART_MERGE = "lock:cart:merge:{memberId}:{mergeId}";
}
