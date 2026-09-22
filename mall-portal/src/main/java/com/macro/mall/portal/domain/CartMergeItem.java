package com.macro.mall.portal.domain;

import java.io.Serializable;

/**
 * 购物车合并条目。为什么单独定义：Cookie 只传递合并所需的最小字段，避免把数据库实体直接暴露给前端。
 */
public class CartMergeItem implements Serializable {

    private Long productId;
    private Long productSkuId;
    private Integer quantity;
    private Boolean checked;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getProductSkuId() {
        return productSkuId;
    }

    public void setProductSkuId(Long productSkuId) {
        this.productSkuId = productSkuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }
}