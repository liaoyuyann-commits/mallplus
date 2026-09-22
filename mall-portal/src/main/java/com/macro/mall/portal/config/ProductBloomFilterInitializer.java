package com.macro.mall.portal.config;

import com.macro.mall.common.cache.BloomFilterService;
import com.macro.mall.common.cache.CacheKeyConstant;
import com.macro.mall.mapper.PmsProductMapper;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.model.PmsProductExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品布隆过滤器预热。为什么存在：空过滤器不能用于入口拦截，否则正常商品会被误判为不存在。
 */
@Configuration
public class ProductBloomFilterInitializer {

    @Autowired
    private PmsProductMapper productMapper;
    @Autowired
    private BloomFilterService bloomFilterService;

    @PostConstruct
    public void initialize() {
        PmsProductExample example = new PmsProductExample();
        example.createCriteria().andDeleteStatusEqualTo(0).andPublishStatusEqualTo(1);
        List<String> keys = productMapper.selectByExample(example).stream()
                .map(PmsProduct::getId)
                .map(id -> CacheKeyConstant.PRODUCT_BLOOM + id)
                .collect(Collectors.toList());
        bloomFilterService.initialize(keys);
    }
}