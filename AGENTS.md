# AGENTS.md — MallPlus 项目主指令

> 本文件是所有 AI 助手（及开发者）的**总入口**。任何 AI 开始工作前，必须先读本文件，再按需读 `agent_docs/` 下的细分文档。这是"渐进式披露"：根文件只放概览与状态，细节在 agent_docs/。

## 项目概览

- **项目名:** MallPlus（购物商城系统）—— 基于开源 **mall-swarm**（Spring Cloud 微服务商城）二次开发
- **定位:** Java 后端**面试展示项目**：证明能读懂开源项目、能借助 AI 设计业务模块、了解后端项目从想法到落地的流程
- **两大核心功能:**
  1. **多级缓存治理**：Caffeine 本地缓存 + Redis 二级缓存；互斥锁防击穿；布隆过滤器 + 空值缓存防穿透；随机过期防雪崩；admin 改商品 → RocketMQ → 前台清缓存（一致性）
  2. **购物车双层存储**：未登录 Cookie 临时车 + 已登录 Redis Hash 高性能车；登录异步合并（RocketMQ）；查询/变更 <20ms
- **用户水平:** C 型（Java 基础，学习型）—— 新概念必须先解释再落地，代码注释讲"为什么"
- **关键约束:** 不动框架（Spring Boot 3 + Spring Cloud Alibaba 沿用原版）；不动原版 RabbitMQ（订单延迟消息保留）；新增依赖需先在 PRD/TechDesign 确认

## 技术栈（详见 agent_docs/tech_stack.md）

Java 17 · Spring Boot 3.x · Spring Cloud Alibaba · MySQL（原版）· Redis · Caffeine 3.x · Guava（布隆过滤器）· RocketMQ（新增）· Sa-Token（原版）· Maven

## 当前阶段与任务（与 MEMORY.md 同步，改一处同步另一处）

- **阶段:** MVP 代码开发（缓存治理与购物车合并切片已完成）
- **当前任务:** 静态审查、运行环境验证与后续购物车字段完整化
- **下一步:** 见 MEMORY.md

## 施工顺序（今晚，来自 TechDesign）

0. git 初始化 + 建分支 `feature/cache`、`feature/cart`
1. mall-common 基建（Caffeine/布隆/互斥锁/随机过期/Key 常量 + pom 依赖）
2. 缓存治理：`PmsPortalProductServiceImpl.detail()` + `HomeServiceImpl`
3. 购物车：`CartCookieService` + `CartRedisService` + `OmsCartItemServiceImpl` 双层路由 + `POST /cart/merge`
4. RocketMQ：`RocketMqConfig` + admin `CacheEvictProducer` + portal `CacheEvictReceiver` + `CartMergeConsumer` + mergeId 幂等
5. 静态审查（AI 逐文件核对）+ git 提交推送

当前阶段完成范围：缓存治理、商品缓存失效通知、Cookie 临时车、Redis Hash 合并与查询/加购/清空路由已落地；购物车修改/删除仍保留 MySQL cartId 兼容路径。

## 行为准则（AI 必须遵守）

### 如何思考
1. **先理解意图**：回答/动手前，先确认用户真正要什么
2. **不确定就问**：关键信息缺失时，先问，不猜
3. **先计划后编码**：动代码前给出简短方案，用户批准后再写
4. **改后必验证**：每次改动后跑编译/静态检查/人工核对
5. **讲清权衡**：推荐方案时说明备选和为什么选它

### Plan → Execute → Verify（强制）
- **Plan:** 每个功能块先讲"做什么、为什么、涉及哪些文件"，用户点头后动手
- **Execute:** 一次只做一个功能块，不夹带无关改动
- **Verify:** 每块完成后静态核对（本机暂无 JDK17/Maven，第 1 周补编译验证）

### 禁止事项（What NOT To Do）
- 不删除任何文件/代码，除非用户明确确认
- 不改数据库表结构（本项目沿用原版表，不加表）
- 不添加 PRD/TechDesign 之外的功能
- 不跳过验证直接宣称完成
- 不使用已废弃的 API / 依赖
- 不静默替换用户确认过的设计决策（如缓存 Key 结构、Cookie 字段）

### 工程约束（Java 版）
- **分层纪律:** Controller 只做请求/响应路由；业务逻辑一律在 `service/impl`；不跨层直接调 Mapper
- **依赖治理:** 新加依赖前查 pom 是否已有；优先复用 mall-common 现有 `RedisService`；不引入"锦上添花"的库
- **注释纪律:** 新类/新方法注释讲"为什么"，不讲"是什么"；关键算法（互斥锁、合并幂等）必须注释
- **命名纪律:** 缓存 Key 用 `CacheKeyConstant` 统一管理；RocketMQ topic/tag 用 `RocketMqConstant` 统一管理
- **沟通纪律:** 简洁陈述问题并立即修复，不重复道歉；上下文缺失时只问一个关键问题

## 关键设计决策（勿随意推翻，改前先讨论）

| 决策 | 结论 | 理由 |
|---|---|---|
| 本地缓存 | Caffeine 3.x | Spring Boot 3 兼容、淘汰策略优 |
| 布隆过滤器 | Guava BloomFilter（单实例内存） | 零额外依赖；多实例场景讲 Redisson |
| 互斥锁 | Redis SETNX + 随机 value | 一行实现、面试好讲 |
| 已登录购物车 | Redis Hash（`cart:member:{memberId}`，field=`{productId}:{skuId}`） | 字段级原子操作、并发安全 |
| 未登录购物车 | Cookie（`mallplus_cart_guest`，≤3KB，JSON 精简字段） | 浏览器隔离、无需登录 |
| 登录合并 | 前端登录后调 `POST /cart/merge` 传临时车 JSON → RocketMQ 异步合并；mergeId 幂等 | 后端读不到浏览器 Cookie；异步不阻塞登录 |
| 缓存一致性 | Cache Aside + admin 改商品发 RocketMQ（tag=PRODUCT_EVICT）→ portal 清本地+Redis | 消息丢失有 Caffeine 短过期兜底 |
| MQ | RocketMQ 4.x 新增并存（不替换 RabbitMQ） | 承载失效通知 + 购物车合并 |
| 随机过期 | base + 10% 随机抖动 | 防雪崩 |

## 验证命令（详见 agent_docs/testing.md）

- 编译: `mvn compile -pl mall-portal -am`（第 1 周装好 JDK17/Maven 后执行）
- 打包: `mvn package -pl mall-portal -am -DskipTests`
- 静态审查: AI 逐文件核对（当前阶段主手段）

## 文档索引

- `agent_docs/tech_stack.md` — 技术栈与命令
- `agent_docs/project_brief.md` — 项目定位与范围
- `agent_docs/testing.md` — 验证策略
- `agent_docs/code_patterns.md` — 代码约定（原版 + 新增）
- `document/PRD-MallPlus-MVP.md` — 需求（Part 2）
- `document/TechDesign-MallPlus-MVP.md` — 技术设计（Part 3，施工图）
