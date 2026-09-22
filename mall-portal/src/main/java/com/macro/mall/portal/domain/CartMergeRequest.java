package com.macro.mall.portal.domain;

import java.io.Serializable;
import java.util.List;

/**
 * 登录后购物车合并请求。为什么携带 mergeRequestId：同一次登录重试不能重复叠加商品数量。
 */
public class CartMergeRequest implements Serializable {

    private String mergeRequestId;
    private List<CartMergeItem> items;

    public String getMergeRequestId() {
        return mergeRequestId;
    }

    public void setMergeRequestId(String mergeRequestId) {
        this.mergeRequestId = mergeRequestId;
    }

    public List<CartMergeItem> getItems() {
        return items;
    }

    public void setItems(List<CartMergeItem> items) {
        this.items = items;
    }
}