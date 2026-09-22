# Code Patterns — 代码约定（原版 + 新增）

> 新代码必须遵循原版风格，避免"一眼看出是外来代码"。

## 原版约定（必须模仿）

### 分层结构
```
Controller（只做路由 + @ApiOperation 注释）
  → Service 接口（mall-portal/service 或 mall-admin/service）
    → ServiceImpl（业务逻辑，@Service + @Autowired 注入）
      → Mapper（MyBatis）/ Dao（自定义 SQL）/ 其他 Service
```

### 典型注解
```java
@RestController
@Api(tags = "PmsPortalProductController", description = "前台商品管理")
@RequestMapping("/product")
public class PmsPortalProductController { ... }

@Service
public class XxxServiceImpl implements XxxService {
    @Autowired
    private XxxMapper xxxMapper;
}
```

### 返回类型
- 统一 `CommonResult`（mall-common `com.macro.mall.common.api.CommonResult`），`CommonResult.success(data)` / `CommonResult.failed(...)`
- 分页用 `CommonPage`

### 其他
- 实体类用 MyBatis Generator 生成的 model（lombok `@Data`），**不要手写实体**
- 工具类优先用 **hutool**（`cn.hutool.*`，原版已依赖）和 Spring 自带工具（`org.springframework.util`）
- 注释风格：类头 `/** ... Created by macro on ... */`，方法注释讲用途

## 新增代码约定

### 包与命名
- 公共组件放 `mall-common/com/macro/mall/common/` 下新增 `cache/`、`rocketmq/` 子包
- portal 新组件放 `com.macro.mall.portal.component/`（如 `CacheEvictReceiver`）、新 service 放 `service/` + `service/impl/`

### 注释纪律
- 每个新类头注释：`/** 职责：... 为什么存在：... */`
- 关键算法（互斥锁获取/释放、合并幂等、Hash 原子操作）必须注释"为什么这么做"
- 不写"是什么"的废话注释（如 `// 获取用户`）

### 缓存 Key 管理
```java
// CacheKeyConstant.java（mall-common/cache）
public interface CacheKeyConstant {
    String PRODUCT_DETAIL = "portal:product:detail:{id}";  // 商品详情缓存
    String PRODUCT_EMPTY  = "portal:product:empty:{id}";   // 空值缓存（防穿透兜底）
    String CART_MEMBER    = "cart:member:{memberId}";       // 已登录购物车（Hash）
    String LOCK_PRODUCT   = "lock:portal:product:detail:{id}"; // 互斥锁
    // ... 统一管理，禁止在业务代码里硬编码 key
}
```

### RocketMQ 常量管理
```java
// RocketMqConstant.java（mall-common/rocketmq）
public interface RocketMqConstant {
    String TOPIC_CACHE = "MALL_CACHE";
    String TAG_PRODUCT_EVICT = "PRODUCT_EVICT";  // 商品缓存失效通知
    String TAG_CART_MERGE    = "CART_MERGE";     // 购物车合并事件
    String GROUP_PORTAL_CACHE = "mall-portal-cache-group";
    String GROUP_PORTAL_CART  = "mall-portal-cart-group";
}
```

### 并发模式（写代码时参考）
- 互斥锁：`redisService` SETNX（用 `StringRedisTemplate.opsForValue().setIfAbsent(key, value, Duration)`），value 用 UUID，释放时先比对再删（防误删别人的锁）
- Hash 加购：`redisTemplate.opsForHash()`，数量累加用 `increment`（原子）
- 合并幂等：`setIfAbsent("merge:{memberId}:{mergeId}", "1", 24h)` 判重

### 禁止
- 不在 Controller 写业务逻辑
- 不直接 new Service/工具类（用 @Autowired / @Resource）
- 不引入原版未用且未在 TechDesign 确认的依赖
- 不改原版无关文件（git diff 应该只见新增 + 计划内的修改）
