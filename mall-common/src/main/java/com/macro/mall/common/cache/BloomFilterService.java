package com.macro.mall.common.cache;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Guava BloomFilter 服务，用于缓存穿透防护：过滤明显不存在的 key，减少 Redis 压力。
 */
@Component
public class BloomFilterService {

    private final BloomFilter<String> productBloomFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8), 1000000, 0.01);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public void initialize(Collection<String> keys) {
        keys.forEach(productBloomFilter::put);
        initialized.set(true);
    }

    public void add(String key) {
        productBloomFilter.put(key);
    }

    public boolean mightContain(String key) {
        // 未完成预热时不能直接返回 false，否则会把所有真实商品误判为不存在。
        return !initialized.get() || productBloomFilter.mightContain(key);
    }
}
