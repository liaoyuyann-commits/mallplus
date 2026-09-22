# MEMORY.md — 项目状态记忆

> 仓库级记忆（随 git 走）。AI 每次会话开始时先读本文件，结束前更新本文件。与 AGENTS.md 的"当前阶段与任务"同步。

## 当前任务
- MVP 代码开发（缓存治理 + 购物车合并与基础路由已完成）

## 下一步（按顺序）
1. git init / 确认仓库，建分支 `feature/cache` + `feature/cart`
2. mall-common 新增：`LocalCacheManager`、`BloomFilterService`、`CacheLockUtil`、`ExpireTimeUtil`、`CacheKeyConstant`、`RocketMqConstant`、`RocketMqConfig`
3. pom.xml：mall-common 加 caffeine + guava；mall-portal/admin 加 rocketmq-spring-boot-starter（版本安装时验证）
4. 缓存治理（PmsPortalProductServiceImpl.detail / HomeServiceImpl）
5. 购物车（CartCookieService / CartRedisService / OmsCartItemServiceImpl / merge 接口）
6. RocketMQ 链路（admin Producer / portal Receiver / CartMergeConsumer / 幂等）
7. 静态审查 + git 提交

## 已完成
- Part 1 研究 Prompt 生成（research-mall-swarm.md 未实际执行，对话中已收集全部信息）
- Part 2 PRD 已生成并确认 → `document/PRD-MallPlus-MVP.md`
- Part 3 TechDesign 已生成并确认 → `document/TechDesign-MallPlus-MVP.md`
- 四条链路现状探查完成（商品详情/首页/购物车 MySQL/后台商品管理）
- mall-common 基础缓存切片已落地：新增依赖、统一缓存 key、随机过期工具、本地 Caffeine 管理器、Guava BloomFilter、RocketMQ 常量
- 商品详情和首页已接入 Caffeine + Redis 读链路与商品详情互斥锁降级
- 商品布隆过滤器已支持启动预热、入口判定和更新后的维护
- 已新增 `POST /cart/merge`：Redis Hash 合并、field 去重、商品上架过滤、数量上限、mergeRequestId 幂等、异常降级
- 已新增 `CartRedisService`，集中管理登录购物车 Redis Hash 与合并幂等标记
- `/cart/list` 已支持 Redis Hash 优先、MySQL 兼容回退；促销结算暂时保留 MySQL 完整实体查询
- 已接入 Redis 已迁移购物车的加购和清空；未迁移购物车继续使用 MySQL
- 已完成 Cookie 临时购物车序列化、3KB 限制和登录合并入口接入
- 已完成 RocketMQ 购物车合并生产者/消费者；消费者复用 mergeRequestId 幂等逻辑
- 已初始化 Git，并创建 `feature/cache`、`feature/cart` 分支；当前分支为 `feature/cart`
- 已完成 admin 商品更新 -> RocketMQ -> portal 清理 Caffeine/Redis 商品与首页缓存

## 关键决策记录
- 购物车现状是 **MySQL**（oms_cart_item 表），改造为 Cookie + Redis Hash（Redis 为主，MySQL 落库为延伸点不实现）
- RocketMQ 真引入，双职责：缓存失效通知（PRODUCT_EVICT）+ 购物车合并（CART_MERGE）
- 合并接口由前端触发 `POST /cart/merge`（后端读不到浏览器 Cookie）
- 布隆过滤器用 Guava（单实例）；互斥锁用 SETNX
- 随机过期：base + 10% 抖动

## 阻塞项（Blockers）
- 购物车 `updateQuantity`、`delete` 仍使用 MySQL cartId 兼容路径；Redis 最小条目尚未保存完整 cartId/展示字段
- 尚未执行 git commit/push、Redis/RocketMQ 运行时联调
- C 盘仅 50G → 第 1 周跑通环境：云服务器 2C4G（推荐）或本地精简跑（Nacos+MySQL+Redis+RocketMQ 先行，RabbitMQ/ES/Mongo 后装）
- rocketmq-spring-boot-starter 与 Spring Boot 3 的兼容版本待安装时验证（TBD）

## 待定事项（Open Questions）
- Caffeine 本地缓存过期参数与 Redis 协调值（写代码时定）
- RocketMQ 消费重试/死信策略（先做简单幂等，够用即可）
- 购物车合并是否校验库存（先只做数据合并，库存校验留测试阶段）
