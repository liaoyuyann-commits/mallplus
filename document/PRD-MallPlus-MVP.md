# Product Requirements Document: MallPlus MVP

## Overview

**Product Name:** MallPlus（购物商城系统；工程名保持 `mall-swarm`，基于开源 mall-swarm 二次开发）
**Problem Statement:** 这是一个 Java 后端**面试展示项目**——通过基于真实微服务开源项目的二次开发，证明三件事：能读懂开源项目、能借助 AI 设计一个完整业务模块、了解后端项目从想法到落地的流程。
**MVP Goal:** 完成两大核心功能（多级缓存治理、购物车双层存储）的代码实现，可编译、可讲解、git 提交完整。
**Target Launch:** 今晚代码完成并推送 git；2 周内达到"能运行 + 能讲清"。

## Target Users

### Primary User Profile
**Who:** Java 后端岗位的**面试官**，以及项目作者本人（求职者）。
**Problem:**
- 面试官：需要判断候选人是否具备真实工程能力，而非只会背八股
- 求职者：缺少一个有技术深度、能讲清楚、能扛追问的项目

**Current Solution:** 普通教程项目 / 算法刷题——烂大街、无深度、讲不出权衡。
**Why They'll Switch:** 基于真实微服务商城（Spring Cloud Alibaba 全家桶）二次开发，两个功能都有真实技术深度（缓存治理涉及穿透/击穿/雪崩/一致性四大问题；购物车涉及双层存储架构与异步合并），且每一步权衡都能讲清楚。

### User Persona: 求职者（作者本人）
- **Demographics:** Java 基础 + 算法，未写过完整业务模块；用过 Eclipse / VSCode / DevC++ / PyCharm
- **Tech Level:** Beginner-to-Intermediate（C 型）
- **Goals:** 拿到 Java 后端面试机会；面试中完整讲清两个功能的设计与实现
- **Frustrations:** 知道概念但没写过；怕写了讲不清像背稿；担心改名/环境问题导致项目跑不起来

## User Journey

### The Story
面试官扫过简历，看到"mall-swarm 二次开发"。候选人先一句话说明项目背景（基于开源微服务商城、做了缓存治理和购物车架构改造），然后进入核心问答：**问题是什么 → 方案怎么设计 → 为什么这么选 → 踩了什么坑**。讲完两个功能，面试官追问细节（缓存一致性怎么保证？合并购物车并发怎么办？RocketMQ 为什么不用 RabbitMQ？），候选人一一接住——这就是"成功时刻"。

### Key Touchpoints
1. **Discovery:** 简历项目列表
2. **First Contact:** 面试官查看 git 仓库（README、提交记录粒度、代码注释）
3. **Onboarding:** 自我介绍 30 秒
4. **Core Loop:** 项目深挖问答（Why / How / Trade-off / Pitfall）
5. **Retention:** 扛住追问 → 形成"真做过"的印象分

## MVP Features

### Core Features (Must Have)

#### 1. 多级缓存治理（Caffeine 本地缓存 + Redis 二级缓存）
- **Description:** 在商品详情、首页查询链路上引入两级缓存：本地缓存（Caffeine）扛高并发热点，Redis 作为二级共享缓存，MySQL 只在两级都未命中时访问；缓存异步回填。
- **User Value（面试价值）:** 展示对"缓存三兄弟"（穿透/击穿/雪崩）的完整理解与落地能力
- **Success Criteria:**
  - 商品详情/首页查询先查 Caffeine → 未命中查 Redis → 未命中查 DB，链路完整
  - 本地缓存与 Redis 过期时间可配置、可协调
  - MySQL 读压力显著下降（目标 80%+，测试阶段用压测/日志验证）
- **Priority:** Critical (P0)

#### 2. 缓存击穿防护（互斥锁 Mutex Key）
- **Description:** 热点 key 过期瞬间，只允许一个请求回源 DB 并重建缓存，其他请求等待或走降级，避免请求同时打穿 DB。
- **Success Criteria:**
  - 并发请求同一过期 key 时，DB 仅被访问一次
  - 有等待/超时机制，防止死锁
- **Priority:** Critical (P0)

#### 3. 缓存穿透防护（布隆过滤器 + 空值缓存）+ 随机过期防雪崩
- **Description:** 启动时用商品 ID 预热布隆过滤器，查询前先过滤不存在的 ID；对确实不存在但被查的 key 缓存空值（短过期）；Redis key 过期时间加随机抖动，防止同一时刻大面积过期。
- **Success Criteria:**
  - 不存在的商品 ID 不进入 DB 查询
  - 空值缓存有短过期与防写穿限制
  - 过期时间带随机性（项目现状是固定值，需改造）
- **Priority:** Critical (P0)

#### 4. 购物车双层存储（Cookie 临时车 + Redis Hash 车）
- **Description:** 未登录用户购物车存 Cookie（临时车，Key 结构、大小、过期可控）；已登录用户购物车存 Redis Hash（field 维度高效存取）；查询/变更走对应存储，互不干扰。
- **Success Criteria:**
  - 未登录加购 → Cookie 生效；登录后 → Redis Hash 生效
  - 购物车查询/变更响应稳定 <20ms
  - Cookie 大小有上限保护，Redis key 有用户维度隔离
- **Priority:** Critical (P0)

#### 5. 登录异步合并（RocketMQ）
- **Description:** 用户登录瞬间，将 Cookie 临时车与 Redis 已登录车**异步合并**（去重、数量汇总），不阻塞登录响应；引入 RocketMQ 承载合并事件（真引入，非口头）。
- **Success Criteria:**
  - 登录接口不因合并而变慢（异步）
  - 合并结果正确：同商品数量相加、不同商品保留、并发登录不重复合并
- **Priority:** Critical (P0)

#### 6. 缓存一致性通知（后台改商品 → RocketMQ → 前台清缓存）
- **Description:** 管理员在 mall-admin 修改商品后发送 RocketMQ 消息，mall-portal 收到后主动清理该商品的本地缓存（Caffeine）与 Redis 缓存，保证用户看到最新数据。
- **Success Criteria:**
  - 后台改商品后，前台商品详情立即读到新数据（缓存已失效）
  - 消息丢失/重复有兜底（本地缓存短过期 + 消费幂等）
- **Priority:** Critical (P0)

## Out of Scope (Not in MVP)

| Feature | Why Wait | Planned For |
|---------|----------|-------------|
| 产品内 AI 交互（AI 客服/推荐） | 预留扩展口（功能接口与实现解耦），本次只留设计 | Version 2 |
| Elasticsearch 搜索深度改造 | 原版已集成 ES 基础搜索，够用 | Version 2 |
| 秒杀/优惠券/分销等重业务 | 复杂度高，面试聚焦两大核心 | Version 2 |
| 真实上线部署/真实收款 | 纯展示项目，不接真实支付渠道 | Never（展示用） |

## Success Metrics

### Primary Metrics
1. **代码可编译 + git 提交完整**：今晚达成
   - How to measure: 装 JDK17 + Maven 后编译通过；git log 每个功能独立提交
   - Why it matters: 证明工程完成度，非半成品
2. **能独立讲清两个功能的设计与实现**：2 周内达成
   - How to measure: 模拟面试问答（Why/How/Trade-off/Pitfall 各 3-5 问）
   - Why it matters: 面试核心目标

### Secondary Metrics
- MySQL 读压力下降 ≥80%（缓存命中率验证）
- 购物车查询/变更 API 响应 <20ms

## UI/UX Direction

**Design Feel:** 干净、专业、工程化（后端项目，前端复用 mall 原版）
**Inspiration:** mall 官方前端（mall-admin-web / mall-app 概念）

### Key Screens（后端接口视角）
1. **商品详情接口**：多级缓存查询链路
2. **首页聚合接口**：缓存治理落地
3. **购物车接口**：Cookie/Redis 双层路由 + 登录合并
4. **后台商品管理**：修改商品 → 缓存失效通知

### Design Principles
- 代码可读性：新类都要有注释，注释讲"为什么"不讲"是什么"
- git 提交按功能单元切分，方便面试官看历史
- 每个改动点能对应到面试讲稿里的一个知识点

## Technical Considerations

**Platform:** Web（微服务后端，Spring Cloud Alibaba）
**Responsive:** 不涉及（前端复用原版）
**Performance Goals:**
- MySQL 读压力下降 ≥80%（测试阶段验证）
- 购物车查询/变更响应 <20ms
- 商品详情接口 P99 显著优于直接查 DB（测试阶段对比）

**Security/Privacy:** 购物车按用户维度隔离（登录态区分）；Cookie 大小有上限；布隆过滤器只存商品 ID 不存敏感数据
**Scalability:** 缓存治理降低 DB 压力，支撑微服务水平扩展；本地缓存按实例分布，配合消息通知解决一致性问题

## AI / Automation Scope

**AI Surface:** 仅开发辅助（AI 协助设计、写代码、排错）；**产品内无 AI 功能**
**预留口设计:** 功能接口与实现解耦（如购物车合并、商品推荐留 Service 接口），未来若要加 AI 客服/推荐，可在接口层扩展，不动主链路
**Allowed Data:** AI 开发工具可读取项目代码；无真实用户数据
**Provider / Retention:** N/A（产品内无 AI，无需 provider/retention 设置）
**Output Contract:** N/A
**Confirmation Rules:** N/A
**Verification Prompts:** N/A（如未来加入产品内 AI，再按"数据边界 + 权限 + 降级 + eval"补全）

## Constraints & Requirements

### Budget
- Development tools: $0/月（Eclipse + 免费开源依赖）
- Hosting/Infrastructure: $0/月（本地运行，不上线）
- Third-party services: $0/月
- **Total: $0/月**

### Timeline
- MVP Development（今晚）: 两大功能 + RocketMQ + 一致性通知，代码写完并推 git
- Beta Testing（第 1 周）: 装 JDK17/Maven、跑中间件（Nacos/MySQL/Redis/RabbitMQ/RocketMQ/ES/MongoDB）、编译排错、跑通
- 理解 + 模拟面试（第 2 周）: 逐行理解代码，模拟面试追问
- Launch Target: 2 周内"能跑 + 能讲"

### Technical Constraints
- 本机现只有 JRE 8（`C:\Program Files\Java\jre1.8.0_441`），项目需 **JDK 17** → 需安装
- Maven 未安装 → 需安装
- 中间件全部走免费本地方案（Docker 或直接安装）
- RocketMQ 为真引入（新增依赖 + 服务端 + 生产/消费端）

## Open Questions & Assumptions

- **假设:** 布隆过滤器数据预热用商品 ID 全量加载（mall 商品量级适合启动/定时预热）
- **假设:** RocketMQ 双用途：缓存失效通知（admin→portal）+ 登录购物车合并事件
- **待定:** RocketMQ 消息幂等与重复消费的处理策略（写代码时定）
- **待定:** Caffeine 本地缓存过期参数与 Redis 的协调值（写代码时定）
- **待定:** 购物车合并时库存校验是否纳入（先做数据合并，库存校验留测试阶段）

## Quality Standards

**Code Quality:**
- 新增类都有注释，注释讲"为什么"（这是面试讲稿的底稿）
- 不复制不理解粘贴的代码——每个新概念先解释再落地
- git 提交按功能单元切分（feature/cache, feature/cart 等）

**Design Quality:**
- 面试能讲清：每个功能 = 问题 → 方案 → 权衡 → 坑
- README 更新：项目介绍 + 两大功能说明 + 架构简述

**What This Project Will NOT Accept:**
- 写了但讲不清的代码（写之前先讲给我听）
- 半成品功能（要么完整，要么不写）
- 没有 git 历史的"一次性粘贴"

## Risk Mitigation

| Risk | Impact | Mitigation Strategy |
|------|--------|-------------------|
| 今晚写不完（工作量超预期） | Medium | 先骨架后细节，按文件清单推进；缓存治理优先，购物车随后 |
| 无本地编译环境，代码可能有编译错误 | High | 我逐文件静态审查；装 JDK17/Maven 后第一件事就是编译 |
| 缓存一致性逻辑出错（改商品看不到新数据） | Medium | 设计先行：明确 Cache Aside 更新顺序 + 消息通知 + 短过期兜底 |
| RocketMQ 引入失败（服务端装不上） | Medium | 备选：购物车合并改线程池异步，消息仅用于清缓存；或消息改 RabbitMQ |
| 面试追问答不上来 | High | 第 2 周安排模拟面试专项（按 Why/How/Trade-off/Pitfall 四类问题） |

## MVP Completion Checklist

### Development Complete
- [ ] 6 个 P0 功能代码完成（缓存治理 3 项 + 购物车 2 项 + 一致性通知 1 项）
- [ ] 静态审查通过（无编译级错误）
- [ ] git 推送成功（提交按功能切分）

### Launch Ready（能讲）
- [ ] 每个功能能讲 Why / How / Trade-off / Pitfall
- [ ] 模拟面试通过一轮
- [ ] README 更新（项目介绍 + 功能说明）

### Quality Checks
- [ ] 装环境后代码编译通过
- [ ] 核心流程跑通（商品详情走缓存、购物车双层路由、登录合并）
- [ ] 性能指标验证（DB 压力下降、购物车 <20ms）

## Next Steps

1. **Immediate:** 读 mall-swarm 项目结构，定位商品详情/首页/购物车/商品管理四条链路（我带你读）
2. **Next:** 今晚动工——按文件清单写代码，边写边讲
3. **Then:** 第 1 周装环境、编译、跑通
4. **Build:** 第 2 周逐行理解 + 模拟面试
5. **Launch:** 投简历

---
*Created: 2026-09-23*
*Status: Draft — Ready for Technical Design*

---
## Handoff Context
<!-- Machine-readable summary for the next workflow step. Do not delete; the next prompt in the workflow reads this block. -->
- Stage: prd
- App name: MallPlus（购物商城系统，基于 mall-swarm 二次开发）
- User level: C  (A = vibe coder, B = developer, C = in-between)
- Target platform: web
- Budget: 免费
- Timeline: 今晚代码完成推 git，2 周内能跑能讲
- Source files: research-mall-swarm.md → PRD-MallPlus-MVP.md

```json
{
  "schemaVersion": 1,
  "documentType": "prd",
  "appName": "MallPlus",
  "oneLiner": "基于 mall-swarm 二次开发的面试项目：多级缓存治理 + 购物车双层存储",
  "targetUsers": "Java 后端面试官 + 求职者本人",
  "phase": "Foundation",
  "mustHave": ["二级缓存治理", "缓存击穿防护", "缓存穿透防护", "购物车双层存储", "登录异步合并", "缓存一致性通知"],
  "niceToHave": ["性能压测数据", "README 完善"],
  "notInMvp": ["产品内 AI 交互", "搜索改造", "秒杀优惠券", "上线部署"],
  "successMetrics": ["代码可编译", "git 提交完整", "能讲清两个功能设计实现"]
}
```
