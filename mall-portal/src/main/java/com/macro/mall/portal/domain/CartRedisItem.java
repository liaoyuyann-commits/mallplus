package com.macro.mall.portal.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 已登录购物车 Redis Hash 的单条 value。
 * 为什么单独定义：Redis 需要保存完整商品展示字段（名称/图/价/规格等），而 CartMergeItem 只承载
 * Cookie 与合并请求的最小字段；若把展示字段塞进 CartMergeItem，会把 3KB 的访客 Cookie 撑爆。
 * 因此「线上字段（CartMergeItem）」与「Redis 存储字段（本类）」分开。
 */
@Data
public class CartRedisItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品 id */
    private Long productId;
    /** 商品 sku id（无规格为 null） */
    private Long productSkuId;
    /** 购买数量 */
    private Integer quantity;
    /** 是否勾选 */
    private Boolean checked;

    /** 加入购物车时的价格（展示用） */
    private BigDecimal price;
    /** 商品名称 */
    private String productName;
    /** 商品主图 */
    private String productPic;
    /** 商品副标题（卖点） */
    private String productSubTitle;
    /** sku 条码 */
    private String productSkuCode;
    /** 品牌名称 */
    private String productBrand;
    /** 商品货号 */
    private String productSn;
    /** 销售属性 JSON（规格） */
    private String productAttr;
    /** 商品分类 id */
    private Long productCategoryId;
    /** 加入购物车时间 */
    private Date createTime;
}
