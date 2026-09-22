package com.macro.mall.portal.domain;

import java.io.Serializable;

/**
 * RocketMQ 购物车合并消息。
 */
public class CartMergeMessage implements Serializable {

    private Long memberId;
    private CartMergeRequest request;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public CartMergeRequest getRequest() {
        return request;
    }

    public void setRequest(CartMergeRequest request) {
        this.request = request;
    }
}
