package com.macro.mall.portal.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.macro.mall.mapper.OmsCartItemMapper;
import com.macro.mall.mapper.PmsProductMapper;
import com.macro.mall.mapper.PmsSkuStockMapper;
import com.macro.mall.model.OmsCartItem;
import com.macro.mall.model.OmsCartItemExample;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.model.PmsSkuStock;
import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.dao.PortalProductDao;
import com.macro.mall.portal.domain.CartProduct;
import com.macro.mall.portal.domain.CartPromotionItem;
import com.macro.mall.portal.domain.CartMergeItem;
import com.macro.mall.portal.domain.CartMergeRequest;
import com.macro.mall.portal.domain.CartRedisItem;
import com.macro.mall.portal.service.OmsCartItemService;
import com.macro.mall.portal.service.OmsPromotionService;
import com.macro.mall.portal.service.CartRedisService;
import com.macro.mall.portal.service.UmsMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车管理Service实现类
 * Created by macro on 2018/8/2.
 */
@Service
public class OmsCartItemServiceImpl implements OmsCartItemService {
    private static final int MAX_QUANTITY = 99;

    @Autowired
    private OmsCartItemMapper cartItemMapper;
    @Autowired
    private PortalProductDao productDao;
    @Autowired
    private OmsPromotionService promotionService;
    @Autowired
    private UmsMemberService memberService;
    @Autowired
    private CartRedisService cartRedisService;
    @Autowired
    private PmsProductMapper productMapper;
    @Autowired
    private PmsSkuStockMapper skuStockMapper;

    @Override
    public int add(OmsCartItem cartItem) {
        UmsMember currentMember =memberService.getCurrentMember();
        cartItem.setMemberId(currentMember.getId());
        cartItem.setMemberNickname(currentMember.getNickname());
        cartItem.setDeleteStatus(0);
        if (cartRedisService.hasItems(currentMember.getId())) {
            CartRedisItem redisItem = new CartRedisItem();
            redisItem.setProductId(cartItem.getProductId());
            redisItem.setProductSkuId(cartItem.getProductSkuId());
            redisItem.setQuantity(Math.min(MAX_QUANTITY, cartItem.getQuantity()));
            redisItem.setChecked(true);
            // 展示字段从 DB 取权威值，不信任请求体里的价格/名称
            fillProductFields(redisItem, productMapper.selectByPrimaryKey(cartItem.getProductId()));
            Map<String, CartRedisItem> items = cartRedisService.readItems(currentMember.getId());
            String field = toCartField(redisItem.getProductId(), redisItem.getProductSkuId());
            CartRedisItem existingItem = items.get(field);
            if (existingItem == null) {
                items.put(field, redisItem);
            } else {
                existingItem.setQuantity(Math.min(MAX_QUANTITY,
                        existingItem.getQuantity() + redisItem.getQuantity()));
            }
            cartRedisService.writeItems(currentMember.getId(), items);
            return 1;
        }

        int count;
        OmsCartItem existCartItem = getCartItem(cartItem);
        if (existCartItem == null) {
            cartItem.setCreateDate(new Date());
            count = cartItemMapper.insert(cartItem);
        } else {
            cartItem.setModifyDate(new Date());
            existCartItem.setQuantity(existCartItem.getQuantity() + cartItem.getQuantity());
            count = cartItemMapper.updateByPrimaryKey(existCartItem);
        }
        return count;
    }

    /**
     * 根据会员id,商品id和规格获取购物车中商品
     */
    private OmsCartItem getCartItem(OmsCartItem cartItem) {
        OmsCartItemExample example = new OmsCartItemExample();
        OmsCartItemExample.Criteria criteria = example.createCriteria().andMemberIdEqualTo(cartItem.getMemberId())
                .andProductIdEqualTo(cartItem.getProductId()).andDeleteStatusEqualTo(0);
        if (cartItem.getProductSkuId()!=null) {
            criteria.andProductSkuIdEqualTo(cartItem.getProductSkuId());
        }
        List<OmsCartItem> cartItemList = cartItemMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(cartItemList)) {
            return cartItemList.get(0);
        }
        return null;
    }

    @Override
    public List<OmsCartItem> list(Long memberId) {
        List<CartRedisItem> redisItems = cartRedisService.listItems(memberId);
        if (!redisItems.isEmpty()) {
            List<OmsCartItem> cartItems = new ArrayList<>();
            for (CartRedisItem redisItem : redisItems) {
                cartItems.add(toCartItem(memberId, redisItem));
            }
            return cartItems;
        }
        return listFromDatabase(memberId);
    }

    private List<OmsCartItem> listFromDatabase(Long memberId) {
        OmsCartItemExample example = new OmsCartItemExample();
        example.createCriteria().andDeleteStatusEqualTo(0).andMemberIdEqualTo(memberId);
        return cartItemMapper.selectByExample(example);
    }

    @Override
    public List<CartPromotionItem> listPromotion(Long memberId, List<Long> cartIds) {
        // Redis 合并条目只保存最小字段，促销计算暂时继续使用 MySQL 中的完整购物车实体。
        List<OmsCartItem> cartItemList = listFromDatabase(memberId);
        if(CollUtil.isNotEmpty(cartIds)){
            cartItemList = cartItemList.stream().filter(item->cartIds.contains(item.getId())).collect(Collectors.toList());
        }
        List<CartPromotionItem> cartPromotionItemList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(cartItemList)){
            cartPromotionItemList = promotionService.calcCartPromotion(cartItemList);
        }
        return cartPromotionItemList;
    }

    @Override
    public List<CartRedisItem> merge(Long memberId, CartMergeRequest request) {
        if (memberId == null || request == null || request.getMergeRequestId() == null
                || request.getMergeRequestId().trim().isEmpty()) {
            return new ArrayList<>();
        }

        boolean firstRequest = cartRedisService.markMergeRequest(memberId, request.getMergeRequestId());
        if (!firstRequest) {
            return cartRedisService.listItems(memberId);
        }

        try {
            Map<String, CartRedisItem> mergedItems = cartRedisService.readItems(memberId);
            if (request.getItems() != null) {
                for (CartMergeItem guestItem : request.getItems()) {
                    PmsProduct product = resolveMergeableProduct(guestItem);
                    if (product == null) {
                        continue;
                    }
                    String field = toCartField(guestItem.getProductId(), guestItem.getProductSkuId());
                    CartRedisItem target = mergedItems.get(field);
                    if (target == null) {
                        target = new CartRedisItem();
                        target.setProductId(guestItem.getProductId());
                        target.setProductSkuId(guestItem.getProductSkuId());
                        target.setQuantity(Math.min(MAX_QUANTITY, guestItem.getQuantity()));
                        target.setChecked(Boolean.TRUE.equals(guestItem.getChecked()));
                        // 合并时展示字段同样以 DB 为准，不信任 Cookie 传来的值
                        fillProductFields(target, product);
                        mergedItems.put(field, target);
                    } else {
                        target.setQuantity(Math.min(MAX_QUANTITY,
                                target.getQuantity() + guestItem.getQuantity()));
                        target.setChecked(Boolean.TRUE.equals(target.getChecked())
                                && Boolean.TRUE.equals(guestItem.getChecked()));
                    }
                }
            }
            cartRedisService.writeItems(memberId, mergedItems);
            return new ArrayList<>(mergedItems.values());
        } catch (RuntimeException exception) {
            cartRedisService.clearMergeRequest(memberId, request.getMergeRequestId());
            return cartRedisService.listItems(memberId);
        }
    }

    /**
     * 校验合并条目是否可并入（商品存在、未删除、已上架、sku 归属正确），并返回商品实体复用。
     * 返回 null 表示不可合并。
     */
    private PmsProduct resolveMergeableProduct(CartMergeItem item) {
        if (item == null || item.getProductId() == null || item.getQuantity() == null
                || item.getQuantity() <= 0) {
            return null;
        }
        PmsProduct product = productMapper.selectByPrimaryKey(item.getProductId());
        if (product == null || !Integer.valueOf(0).equals(product.getDeleteStatus())
                || !Integer.valueOf(1).equals(product.getPublishStatus())) {
            return null;
        }
        if (item.getProductSkuId() != null) {
            PmsSkuStock skuStock = skuStockMapper.selectByPrimaryKey(item.getProductSkuId());
            if (skuStock == null || !item.getProductId().equals(skuStock.getProductId())) {
                return null;
            }
        }
        return product;
    }

    /**
     * 用 DB 商品信息回填 Redis 条目的展示字段；价格/名称/图等一律以 DB 为准，不信任前端。
     */
    private void fillProductFields(CartRedisItem item, PmsProduct product) {
        if (product == null) {
            return;
        }
        item.setProductName(product.getName());
        item.setProductPic(product.getPic());
        item.setProductSubTitle(product.getSubTitle());
        item.setPrice(product.getPrice());
        item.setProductBrand(product.getBrandName());
        item.setProductSn(product.getProductSn());
        item.setProductCategoryId(product.getProductCategoryId());
        if (item.getProductSkuId() != null) {
            PmsSkuStock sku = skuStockMapper.selectByPrimaryKey(item.getProductSkuId());
            if (sku != null) {
                item.setProductSkuCode(sku.getSkuCode());
                item.setProductAttr(sku.getSpData());
            }
        }
        if (item.getCreateTime() == null) {
            item.setCreateTime(new Date());
        }
    }

    /**
     * 把 Redis 条目映射回购物车实体，补齐前端展示所需字段。
     */
    private OmsCartItem toCartItem(Long memberId, CartRedisItem item) {
        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setMemberId(memberId);
        cartItem.setProductId(item.getProductId());
        cartItem.setProductSkuId(item.getProductSkuId());
        cartItem.setQuantity(item.getQuantity());
        cartItem.setPrice(item.getPrice());
        cartItem.setProductPic(item.getProductPic());
        cartItem.setProductName(item.getProductName());
        cartItem.setProductSubTitle(item.getProductSubTitle());
        cartItem.setProductSkuCode(item.getProductSkuCode());
        cartItem.setProductBrand(item.getProductBrand());
        cartItem.setProductSn(item.getProductSn());
        cartItem.setProductAttr(item.getProductAttr());
        cartItem.setProductCategoryId(item.getProductCategoryId());
        cartItem.setCreateDate(item.getCreateTime());
        cartItem.setDeleteStatus(0);
        return cartItem;
    }

    private String toCartField(Long productId, Long productSkuId) {
        return productId + ":" + (productSkuId == null ? 0 : productSkuId);
    }

    @Override
    public int updateQuantity(Long productId, Long productSkuId, Long memberId, Integer quantity) {
        if (cartRedisService.hasItems(memberId)) {
            String field = toCartField(productId, productSkuId);
            return cartRedisService.updateQuantity(memberId, field, quantity) ? 1 : 0;
        }
        OmsCartItem probe = new OmsCartItem();
        probe.setMemberId(memberId);
        probe.setProductId(productId);
        probe.setProductSkuId(productSkuId);
        OmsCartItem existCartItem = getCartItem(probe);
        if (existCartItem == null) {
            return 0;
        }
        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setQuantity(quantity);
        OmsCartItemExample example = new OmsCartItemExample();
        example.createCriteria().andDeleteStatusEqualTo(0)
                .andIdEqualTo(existCartItem.getId()).andMemberIdEqualTo(memberId);
        return cartItemMapper.updateByExampleSelective(cartItem, example);
    }

    @Override
    public int delete(Long memberId, List<String> fields) {
        if (cartRedisService.hasItems(memberId)) {
            return cartRedisService.removeItems(memberId, fields).intValue();
        }
        List<Long> cartIds = new ArrayList<>();
        for (String field : fields) {
            Long cartId = resolveCartId(memberId, field);
            if (cartId != null) {
                cartIds.add(cartId);
            }
        }
        if (cartIds.isEmpty()) {
            return 0;
        }
        OmsCartItem record = new OmsCartItem();
        record.setDeleteStatus(1);
        OmsCartItemExample example = new OmsCartItemExample();
        example.createCriteria().andIdIn(cartIds).andMemberIdEqualTo(memberId);
        return cartItemMapper.updateByExampleSelective(record, example);
    }

    /**
     * 把 Redis field（productId:skuId）解析回 MySQL cartId，供未迁移购物车的回退路径使用。
     * field 中 skuId 为 0 表示无规格。
     */
    private Long resolveCartId(Long memberId, String field) {
        String[] parts = field.split(":");
        if (parts.length != 2) {
            return null;
        }
        Long productId;
        Long productSkuId;
        try {
            productId = Long.valueOf(parts[0]);
            productSkuId = "0".equals(parts[1]) ? null : Long.valueOf(parts[1]);
        } catch (NumberFormatException ignored) {
            return null;
        }
        OmsCartItem probe = new OmsCartItem();
        probe.setMemberId(memberId);
        probe.setProductId(productId);
        probe.setProductSkuId(productSkuId);
        OmsCartItem existCartItem = getCartItem(probe);
        return existCartItem == null ? null : existCartItem.getId();
    }

    @Override
    public CartProduct getCartProduct(Long productId) {
        return productDao.getCartProduct(productId);
    }

    @Override
    public int updateAttr(OmsCartItem cartItem) {
        //删除原购物车信息
        OmsCartItem updateCart = new OmsCartItem();
        updateCart.setId(cartItem.getId());
        updateCart.setModifyDate(new Date());
        updateCart.setDeleteStatus(1);
        cartItemMapper.updateByPrimaryKeySelective(updateCart);
        cartItem.setId(null);
        add(cartItem);
        return 1;
    }

    @Override
    public int clear(Long memberId) {
        if (cartRedisService.hasItems(memberId)) {
            cartRedisService.clearItems(memberId);
            return 1;
        }
        OmsCartItem record = new OmsCartItem();
        record.setDeleteStatus(1);
        OmsCartItemExample example = new OmsCartItemExample();
        example.createCriteria().andMemberIdEqualTo(memberId);
        return cartItemMapper.updateByExampleSelective(record,example);
    }
}
