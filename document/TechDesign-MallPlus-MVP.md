# Technical Design Document: MallPlus MVP

## Overview

本文档定义 MallPlus（mall-swarm 二次开发）两个核心功能**怎么建**——是今晚写代码的施工图。

**目标：** 今晚写完 6 个 P0 功能的可编译代码并推 git；第 1 周跑通（C 盘 50G 吃紧 → 云服务器或本地精简跑）；第 2 周理解 + 模拟面试。
**开发环境：** 本机 Eclipse + 需装 JDK 17 + Maven；中间件环境第 1 周再搭（今晚纯写代码，不需要）。
**原则：** 不动框架（Spring Boot 3 + Spring Cloud Alibaba 沿用原版）、不动原版 RabbitMQ（订单延迟消息保留）、新增组件尽量收敛在业务层，风险可控。

## Recommended Approach

### Best Path: 全代码 + AI 协同（learn-by-doing）
- **为什么:** 用户要"核心业务协同完成、一边写一边学"；基于真实开源项目改造，面试含金量最高；改动集中在业务层（Service/Component），不碰框架，回滚风险低
- **Time:** 今晚代码 8-10 小时；第 1 周跑通；第 2 周能讲
- **Learning curve:** 中（每个新概念先解释后落地）
- **Cost:** $0

### 关键选型对比（每个决策 = 备选 + 权衡）

| 决策点 | 选项 | 取舍 | 结论 |
|---|---|---|---|
| 本地缓存 | ① **Caffeine**（推荐）② Guava Cache ③ 自研 | Caffeine 是 Guava 的现代替代（异步加载、淘汰策略更优），Spring Boot 3 官方兼容；自研无必要 | **Caffeine 3.x** |
| 布隆过滤器 | ① **Guava BloomFilter**（推荐）② Redisson RBloomFilter | 单实例运行 Guava 足够且零依赖；Redisson 支持分布式但引入重依赖；面试讲"多实例用 Redisson"即可 | **Guava** |
| 互斥锁 | ① **Redis SETNX**（推荐）② Redisson Lock ③ synchronized | SETNX 一行实现 + 讲清原理（面试最爱问）；Redisson 更严谨但加依赖；synchronized 只限单实例 | **SETNX + 随机 value 防误删** |
| 购物车已登录存储 | ① **Redis Hash**（推荐）② 继续 MySQL+缓存 ③ Redis String JSON | Hash 字段维度高效读写/原子增减，符合"高性能车"目标；纯 String 存整个列表并发写易冲突 | **Redis Hash** |
| 购物车未登录存储 | ① **Cookie**（推荐）② Redis + clientKey | Cookie 天然按浏览器隔离、无需登录；Redis+clientKey 会留垃圾数据 | **Cookie（精简字段 ≤3KB）** |
| MQ | ① **RocketMQ**（真引入，推荐）② 继续 RabbitMQ ③ 线程池 | RocketMQ 承担「缓存失效通知 + 登录合并」双职责，面试可讲两个场景；RabbitMQ 是原版已有但已用于订单；线程池为降级备选 | **RocketMQ 4.x** |
| 跑通环境 | ① **云服务器 2C4G**（推荐）② 本地精简跑 ③ 本地全量跑 | C 盘仅 50G，全量中间件放本地吃紧；云上部署面试可讲"云部署"；本地精简跑仅启动核心链路 | **第 1 周再定** |

### 诚实承认的 trade-off
- Redis Hash 购物车（不落 MySQL）→ Redis 宕机丢购物车数据。面试延伸点："生产级会加异步落库兜底"，写代码时预留落库扩展点，不实现（今晚做不完）
- 布隆过滤器有误判率 → 配合空值缓存兜底（误判=多查一次 DB，无害）
- RocketMQ 引入增加部署负担 → 服务端内存调优至 512M-1G

## 现状梳理（已探查的真实代码）

| 链路 | 现有实现 | 改造点 |
|---|---|---|
| 商品详情 | `PmsPortalProductController` → `PmsPortalProductServiceImpl.detail(id)` 直接查 DB | **核心接入点**：加双级缓存+互斥+布隆+空值 |
| 首页 | `HomeController` → `HomeServiceImpl.content()/hotProductList()/newProductList()` 直接查 DB | 加缓存（含随机过期） |
| 购物车 | `OmsCartItemServiceImpl` → **MySQL `oms_cart_item` 表**（`OmsCartItemMapper`） | **存储迁移**：Cookie 临时车 + Redis Hash 车 |
| 后台商品 | `PmsProductController.update/create/状态变更` → `PmsProductServiceImpl` | 改/增/状态变化后发 RocketMQ 通知清缓存 |
| Redis 基础 | mall-common 已有 `RedisService`（set/get/expire，固定过期） | 复用；随机过期在写入处新增工具处理 |

## 总体架构（新增部分）

```
                        ┌──────────────────────────────┐
                        │        mall-gateway          │
                        └──────────────┬───────────────┘
            ┌───────────────────────────┼───────────────────────────┐
            ▼                           ▼                           ▼
   ┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
   │   mall-portal    │       │   mall-admin     │       │  mall-auth 等    │
   │ 前台服务(核心)    │       │  后台服务        │       │  (原版不动)       │
   └──┬───────┬───────┘       └───────┬──────────┘       └──────────────────┘
      │       │                       │
      │ 新增   │ 新增                  │ 新增
      │ 缓存治理│ 购物车               │ CacheEvictProducer
      │ 组件   │ 组件                 │ (改商品发消息)
      ▼       ▼                       ▼
   Caffeine   Redis Hash         ┌──────────────────────┐
   (本地)      (已登录车)          │  RocketMQ Broker     │
      │       ▲                  │  topic: MALL_CACHE   │
      │       │                  │  tag: PRODUCT_EVICT  │
      │       │ Cookie 临时车     │  tag: CART_MERGE     │
      │       │ (浏览器侧)        └──────────┬───────────┘
      │       │                             │ 消费
      ▼       ▼                             ▼
   ┌──────────────────────────────────────────────────────┐
   │  MySQL（原版沿用） + Redis（原版沿用）                 │
   └──────────────────────────────────────────────────────┘
```

### 三条关键数据流

**① 商品详情读链路（缓存治理）**
```
GET /product/detail/{id}
→ 布隆过滤器.mightContain(id) == false → 直接返回空（防穿透）
→ Caffeine 命中 → 返回
→ Redis 命中 → 回填 Caffeine → 返回
→ 未命中 → SETNX 互斥锁（抢到者查 DB，未抢到者短暂等待后重查缓存）
→ DB 查询 → 回填 Redis（随机过期）→ 回填 Caffeine → 返回
→ 查不到 → 缓存空值（短过期 60s）
```

**② 后台改商品 → 缓存失效（一致性）**
```
mall-admin PmsProductServiceImpl.update(id,...)
→ 更新 MySQL
→ 发送 RocketMQ（topic=MALL_CACHE, tag=PRODUCT_EVICT, body=productId）
→ mall-portal CacheEvictReceiver 消费
→ 删除 Redis key portal:product:detail:{id} + Caffeine.invalidate(id)
（兜底：Caffeine 本身短过期，即使消息丢失最多脏一段时间）
```

**③ 登录购物车合并**
```
前端登录成功后调用 POST /cart/merge {cookieCart: [...临时车JSON], memberId}
→ 后端（OmsCartItemController）收到
→ 发 RocketMQ（topic=MALL_CACHE, tag=CART_MERGE, body=memberId+cookieCart）
→ CartMergeConsumer 消费：逐条并入 Redis Hash 车（同 productId+skuId 数量相加）
→ 幂等：memberId + 合并请求唯一ID（前端生成）去重
```

## 模块设计

### mall-common（新增公共能力）

| 新文件 | 职责 |
|---|---|
| `common/cache/LocalCacheManager.java` | Caffeine 封装：get/put/invalidate/invalidateAll，按 CacheName 管理，过期可配 |
| `common/cache/BloomFilterService.java` | Guava BloomFilter 封装：init(数据源)/mightContain/put；商品 ID 预热 |
| `common/cache/CacheLockUtil.java` | Redis SETNX 互斥锁：tryLock(key, expireSec)/unlock(key, value)；随机 value 防误删 |
| `common/cache/ExpireTimeUtil.java` | 随机过期：base + random(10%)，防雪崩 |
| `common/cache/CacheKeyConstant.java` | 缓存 Key 前缀常量统一管理 |
| `common/rocketmq/RocketMqConfig.java` | Producer/Consumer 配置 Bean（默认 Producer） |
| `common/rocketmq/RocketMqConstant.java` | topic/tag/消费组常量 |

### mall-portal（核心战场：新增 + 修改）

| 文件 | 动作 | 说明 |
|---|---|---|
| `component/CacheEvictReceiver.java` | 新增 | RocketMQ 消费 PRODUCT_EVICT：删 Redis key + Caffeine invalidate |
| `component/CartMergeConsumer.java` | 新增 | RocketMQ 消费 CART_MERGE：执行购物车合并 |
| `service/CartCookieService.java` + impl | 新增 | Cookie 临时车读写：读请求 Cookie、写响应 Cookie、JSON 序列化、容量保护（≤3KB） |
| `service/CartRedisService.java` + impl | 新增 | Redis Hash 车：hPut/hGet/hEntries/hDelete/expire，field=productId:skuId，value=精简 JSON |
| `service/impl/PmsPortalProductServiceImpl.java` | **修改** | `detail()` 加布隆过滤+双级缓存+互斥锁+空值缓存（改造核心） |
| `service/impl/HomeServiceImpl.java` | 修改 | `content()/hotProductList()/newProductList()` 加缓存（随机过期） |
| `service/impl/OmsCartItemServiceImpl.java` | **修改** | `add/list/updateQuantity/delete/clear` 双层路由：登录→Redis Hash，未登录→（Controller 层处理 Cookie）；`list()` 支持 Cookie 数据源 |
| `controller/OmsCartItemController.java` | 修改 | 未登录加购/查车走 Cookie；新增 `POST /cart/merge` 合并接口 |
| `service/impl/UmsMemberServiceImpl.java` | 修改 | 登录成功后（无操作，合并由前端触发 merge 接口）——若需服务端触发再调整 |

### mall-admin（新增 + 修改）

| 文件 | 动作 | 说明 |
|---|---|---|
| `component/CacheEvictProducer.java` | 新增 | 封装发送 PRODUCT_EVICT 消息（productId） |
| `service/impl/PmsProductServiceImpl.java` | **修改** | `create/update/updatePublishStatus/updateDeleteStatus` 成功后发消息 |

### 依赖新增（pom.xml）
- `mall-common`：`com.github.ben-manes.caffeine:caffeine:3.x`、`com.google.guava:guava:33.x`（布隆）
- `mall-portal` + `mall-admin`：`org.apache.rocketmq:rocketmq-spring-boot-starter:2.3.x`（**安装时验证与 Spring Boot 3 兼容版本**，标 TBD）

## 关键设计细节

### 缓存 Key 设计

| 缓存 | Key | 过期 |
|---|---|---|
| 商品详情 | `portal:product:detail:{productId}` | 30min + 随机 |
| 首页内容 | `portal:home:content` | 5min + 随机 |
| 热销商品 | `portal:product:hot:{pageNum}:{pageSize}` | 10min + 随机 |
| 新品 | `portal:product:new:{pageNum}:{pageSize}` | 10min + 随机 |
| 空值缓存 | `portal:product:empty:{productId}` | 60s 固定 |
| 互斥锁 | `lock:portal:product:detail:{productId}` | 3s |
| 已登录购物车 | `cart:member:{memberId}`（Hash） | 30 天滚动 |
| 布隆过滤器 | 内存内 Guava（单实例） | 启动预热 |

### Cookie 临时车设计
- Cookie 名：`mallplus_cart_guest`
- 内容：JSON 数组 `[{productId, skuId, quantity, price, name, pic}]`（精简字段）
- 上限：≤3KB（超限拒绝加购并提示）；过期：30 天
- 读写位置：`OmsCartItemController`（未登录时），Service 层不碰 HttpServletRequest/Response（保持可测试）

### Redis Hash 购物车设计
- Key：`cart:member:{memberId}`
- Field：`{productId}:{skuId}`
- Value：`{quantity, price, name, pic, createTime}`
- 操作：加购 `hPut`（已有 field 则 `hIncrement` 数量——Hash 原子自增，并发安全）；查车 `hEntries`；改数量 `hPut` 覆盖；删除 `hDelete`

### 登录合并幂等设计
- 前端生成 `mergeId`（UUID），随合并请求传入
- Redis `setIfAbsent(merge:{memberId}:{mergeId}, 1, 24h)` 判重，已存在则跳过（防重复消费/重复合并）

### RocketMQ 设计
- topic：`MALL_CACHE`
- tag：`PRODUCT_EVICT`（商品失效）、`CART_MERGE`（购物车合并）
- 消费组：`mall-portal-cache-group`、`mall-portal-cart-group`
- 消费幂等：PRODUCT_EVICT 天然幂等（删缓存）；CART_MERGE 用 mergeId 判重

## 今晚施工顺序（Roadmap）

| 顺序 | 工作块 | 预估 |
|---|---|---|
| 0 | git 初始化/确认仓库，建分支 `feature/cache` + `feature/cart` | 10min |
| 1 | mall-common 基建：Caffeine/布隆/互斥锁/随机过期/Key 常量 + pom 依赖 | 60min |
| 2 | 缓存治理：PmsPortalProductServiceImpl.detail() + HomeServiceImpl | 90min |
| 3 | 购物车：CartCookieService + CartRedisService + OmsCartItemServiceImpl 双层路由 + Controller merge | 120min |
| 4 | RocketMQ：RocketMqConfig + CacheEvictProducer（admin）+ CacheEvictReceiver（portal）+ CartMergeConsumer（portal）+ 幂等 | 90min |
| 5 | 静态审查（我逐文件核对）+ git 提交推送 | 30min |
| **合计** | | **约 6.5-8h** |

> 每个工作块写完即向用户讲解该块设计（Why/How），用户理解后再进下一块。

## 第 1 周跑通（环境方案，C 盘 50G）

**本机必装（编译用，占 ~2-3G）**：JDK 17（Temurin）、Maven 3.9
**中间件（推荐云服务器 2C4G 40G，或本地精简跑）：**

| 中间件 | 用途 | 精简内存配置 |
|---|---|---|
| Nacos | 注册/配置中心（原版依赖） | 默认 512M |
| MySQL | 原版数据 | 500M |
| Redis | 原版 + 购物车/缓存 | 默认 |
| RocketMQ | 新增（失效通知+合并） | nameserver 256M / broker 512M（`-Xms512m -Xmx512m`） |
| RabbitMQ | 原版订单延迟消息（测订单时才需要） | 300M，可后装 |
| Elasticsearch | 原版搜索（测搜索时才需要） | 1G，可后装 |
| MongoDB | 原版浏览记录（可跳过） | 可后装 |

> 跑通顺序：Nacos+MySQL+Redis 先起 → gateway+portal+admin 起来 → 再起 RocketMQ 验证缓存失效和合并 → 最后按需补 RabbitMQ/ES/Mongo。

## 风险与备选

| 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|
| RocketMQ 服务端装不上（内存/网络） | 中 | 中 | 降级：合并改线程池异步；缓存失效改 Redis Pub/Sub 或直接删 Redis key |
| C 盘 50G 不够本地跑 | 高 | 中 | 云服务器 2C4G（Oracle 免费 ARM / 轻量服务器） |
| 本机无 JDK17/Maven，今晚无法编译 | 高 | 高 | 我逐文件静态审查；第 1 周装好后第一件事编译 |
| 缓存一致性 bug（改了看不到新数据） | 中 | 高 | Cache Aside 顺序 + RocketMQ 通知 + Caffeine 短过期兜底 |
| 购物车并发合并重复 | 中 | 中 | mergeId 幂等 + Hash 原子自增 |
| Cookie 体积超限 | 低 | 低 | ≤3KB 限制 + 精简字段 |

## 学习资源（概念 → 资料）

| 概念 | 推荐资料 |
|---|---|
| 缓存三兄弟（穿透/击穿/雪崩） | Redis 官方文档 + 各技术博客"缓存三大问题" |
| Cache Aside 模式 | 美团/阿里技术博客"缓存一致性" |
| Caffeine | GitHub ben-manes/caffeine README |
| Guava BloomFilter | Guava 官方 wiki BloomFilter 页 |
| RocketMQ 快速开始 | rocketmq.apache.org 官方 Quick Start |

## Success Checklist

- [ ] 6 个 P0 功能代码完成（缓存治理 3 + 购物车 2 + 一致性 1）
- [ ] 我逐文件静态审查通过（无编译级错误）
- [ ] git 按功能提交并推送云端
- [ ] （第 1 周）编译通过、核心链路跑通
- [ ] （第 2 周）每个功能能讲 Why/How/Trade-off/Pitfall

---
*Technical Design for: MallPlus*
*Approach: 全代码 + AI 协同（learn-by-doing）*
*Estimated Time to MVP: 今晚代码 + 2 周内能跑能讲*
*Estimated Cost: $0*

---
## Handoff Context
<!-- Machine-readable summary for the next workflow step. Do not delete; the next prompt in the workflow reads this block. -->
- Stage: techdesign
- App name: MallPlus（购物商城系统，基于 mall-swarm 二次开发）
- User level: C  (A = vibe coder, B = developer, C = in-between)
- Target platform: web
- Budget: 免费（$0）
- Timeline: 今晚代码完成推 git，2 周内能跑能讲
- Chosen stack: Java 17 + Spring Boot 3 + Spring Cloud Alibaba（沿用原版）+ MySQL + Redis + Caffeine + Guava + RocketMQ（新增），Eclipse 开发
- AI coding tool: 对话式 AI 协同（本会话），learn-by-doing
- Source files: research-mall-swarm.md → PRD-MallPlus-MVP.md → TechDesign-MallPlus-MVP.md

```json
{
  "schemaVersion": 1,
  "documentType": "techdesign",
  "appName": "MallPlus",
  "stack": {
    "frontend": "复用 mall 原版前端（不开发）",
    "backend": "Spring Boot 3 + Spring Cloud Alibaba + JDK 17",
    "database": "MySQL（原版）+ Redis + Caffeine + Guava BloomFilter",
    "auth": "Sa-Token（原版沿用）",
    "styling": "N/A",
    "deployment": "本地 Eclipse 开发；跑通走云服务器或本地精简"
  },
  "commands": {
    "setup": "mvn install -pl mall-common -am",
    "dev": "mvn spring-boot:run -pl mall-portal（需 Nacos/MySQL/Redis 先行）",
    "test": "mvn test -pl mall-portal",
    "typecheck": "mvn compile",
    "lint": "N/A（Java 后端，代码审查靠静态检查）",
    "build": "mvn package -pl mall-portal -am -DskipTests"
  },
  "aiScope": "none（产品内无 AI；AI 仅用于开发协同）"
}
```
