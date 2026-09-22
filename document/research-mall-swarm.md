## Deep Research Request: mall-swarm 二次开发（缓存治理 + 购物车双层存储）

<context>
我在把开源项目 mall-swarm（Spring Cloud 微服务商城）做二次开发，作为 Java 后端面试项目。
我有 Java 基础，写过普通算法，能看懂简单代码，但没写过完整业务模块。
我需要在 AI 协助下：今晚把两个功能的代码写完（能编译级别的完整代码），之后慢慢测试。

**My Skills:** Java 基础（集合/线程/IO 概念）、Spring Boot 了解一点点、没用过微服务
**Learning Preference:** 希望给出"可以直接照着写"的实现方案 + 每一步解释为什么这么写
**项目现状（已查证）：** mall-portal 用 RabbitMQ（spring-boot-starter-amqp）做订单延迟消息；
Redis 只有基础封装（RedisService：set/get/expire），过期时间是配置固定值；
没有 Caffeine、没有布隆过滤器、没有互斥锁、没有购物车 Redis 外的存储
</context>

<instructions>
### Core Questions:
1. 多级缓存治理：在 mall-portal 商品详情/首页查询链路上，Caffeine 本地缓存 + Redis 二级缓存怎么搭？
   给出：新增哪些类（缓存 Manager、双级缓存 Service、配置类）、缓存读写流程、缓存更新/删除策略、
   本地缓存过期如何与 Redis 协调、后台改商品后如何通知前台清缓存（mall-admin 侧具体改哪里）。
2. 缓存击穿：互斥锁（Mutex Key）模式的具体代码怎么写？在哪个方法上加？
   给出关键代码片段（synchronized / Redis SETNX / Redisson Lock 三种做法的取舍）。
3. 缓存穿透：布隆过滤器怎么落地？Guava BloomFilter vs Redisson RBloomFilter 选哪个？
   布隆过滤器的数据从哪来、什么时候预热、误判了怎么办；空值缓存策略怎么配合。
4. 缓存雪崩防护：随机过期时间怎么加（项目现在固定过期时间，改哪些文件）；其他雪崩防护手段。
5. 缓存一致性：Cache Aside 模式在"商品更新"场景的具体顺序；mall-admin 更新商品后如何让
   mall-portal 的 Redis 和本地缓存失效（直接删 Redis？发消息？）；本地缓存（Caffeine）跨服务怎么失效。
6. 购物车双层存储：未登录 Cookie 临时车怎么设计（Key 结构、Cookie 大小限制、过期时间）；
   已登录 Redis Hash 车怎么设计（field 结构、存储商品什么字段）；登录瞬间异步合并的实现
   （在哪个接口触发、合并算法怎么去重/汇总数量、并发登录会不会重复合并、用线程池还是 MQ）。
7. RocketMQ vs RabbitMQ：项目现状是 RabbitMQ 延迟消息（取消订单）。如果要讲 RocketMQ，
   合理的做法是"了解对比 + 项目继续用 RabbitMQ"还是"真引入 RocketMQ"？两者延迟消息机制差异、
   面试中怎么答取舍最有说服力。给出结论和理由。
8. 改动范围清单：mall-common（公共工具/缓存组件）、mall-portal（前台：缓存治理+购物车）、
   mall-admin（后台：清缓存/管理端）三个模块分别新增/修改哪些文件，给出一页纸清单。
9. 环境与工具：本机只有 JRE 8、无 Maven（有 Eclipse），JDK 17 + Maven 安装有什么坑；
   免费版布隆过滤器/缓存依赖选型；不花钱的本地中间件方案（Nacos/Redis/RabbitMQ 装法简述）。

### Research Areas:
- 缓存架构模式（Cache Aside / 双级缓存）及 mall 系项目的实际落地案例
- 购物车存储设计（Cookie 临时车 + Redis 已登录车）的成熟方案与坑
- 布隆过滤器/互斥锁/空值缓存的代码级实现
- MQ 选型对比（面试导向）
- 面试如何讲清"我做了什么、为什么这么做、有什么坑"

### Specific Focus:
- 每个方案给"最少改动、今晚能写完"的实现路径
- 标注哪些代码可以抄、哪些需要理解后自己写
- 常见坑：缓存穿透导致 DB 被打、合并购物车重复、本地缓存不失效、Cookie 过大
</instructions>

### Required Deliverables:
1. **Feature Matrix** — 两个功能拆成子任务，标注优先级和"今晚必写/可后补"
2. **Tech Stack** — Caffeine/Guava/Redisson/依赖版本建议（匹配 Spring Boot 3 + JDK 17）
3. **改动文件清单** — mall-common/portal/admin 一页纸清单（新增哪些类、改哪些类）
4. **核心代码骨架** — 双级缓存 Service、互斥锁、布隆过滤器初始化、Cookie 购物车、Redis 购物车、
   登录合并方法的关键代码片段（可直接照着写）
5. **Roadmap** — 今晚从零到 git 提交的步骤顺序（含每步预估时间）
6. **Resources** — 每个概念 1-2 个最值得看的免费资料（中文优先）
7. **Budget** — 全程免费方案（依赖、中间件、部署测试）
8. **AI 扩展口设计** — 如果未来产品内要加 AI 功能（如 AI 客服），现在代码里预留什么接口/设计
</instructions>

<output_format>
- 假设读者有 Java 基础但没写过完整业务，关键概念用一句话解释
- **Include source URLs with access dates** for each major recommendation
- Use tables for comparisons（选型对比、改动清单、任务优先级）
- **Note any conflicting information** between sources
- Provide pros/cons for major decisions（如布隆过滤器选型、MQ 取舍）
- 区分"官方文档事实"和"社区/博客经验"
- End the document with this exact block, so the next workflow step can pre-fill instead of re-asking:

```
## Handoff Context
<!-- Machine-readable summary for the next workflow step. Do not delete; the next prompt in the workflow reads this block. -->
- Stage: research
- App name: mall-swarm 二次开发（缓存治理 + 购物车双层存储）
- User level: C  (A = vibe coder, B = developer, C = in-between)
- Target platform: web
- Budget: 免费
- Timeline: 今晚写完代码并推 git，之后慢慢测试
- AI in product scope: no（预留扩展口，后续可能补）
- Source files: research-mall-swarm.md
```
</output_format>
