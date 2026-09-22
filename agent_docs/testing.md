# Testing — 验证策略

> 本机暂无 JDK17/Maven（只有 JRE 8），第 1 周装好前，验证以 **AI 静态审查** 为主；装好后再补编译/运行验证。

## 当前阶段（今晚）— 静态审查
- [ ] 每个新文件：AI 逐行核对（语法、类型、import、与现有代码风格一致）
- [ ] 方法签名与 TechDesign 一致
- [ ] 缓存 Key / topic / tag 均来自常量类，无硬编码
- [ ] 并发安全点核对：SETNX 释放锁是否带 value 校验；Hash 加购是否原子；合并是否幂等
- [ ] 空值/null 处理核对：布隆误判后走空值缓存；Cookie 为空、超限分支

## 第 1 周 — 编译与运行验证
```bash
# 编译（先装 JDK 17 + Maven 3.9）
mvn compile -pl mall-common -am
mvn compile -pl mall-portal -am
mvn compile -pl mall-admin -am
```

### 中间件启动顺序
1. Nacos → MySQL → Redis
2. gateway → auth → portal → admin（能起来、注册成功）
3. RocketMQ nameserver + broker（broker 内存调小）
4. RabbitMQ / ES / Mongo 按需后装

### 功能验证清单
| 功能 | 验证方法 |
|---|---|
| 缓存治理 | 首次请求商品详情走 DB；二次请求命中 Redis/Caffeine（看日志或加临时计数器）；并发压测同一 key 时 DB 只被查一次（互斥锁） |
| 防穿透 | 请求不存在的商品 ID（如 99999999），确认未打 DB（布隆拦截） |
| 随机过期 | 观察 Redis TTL 每次写入略有抖动 |
| 购物车 | 未登录加购 → Cookie 有数据；登录后 → Redis Hash 有数据；merge 后数量正确合并 |
| 一致性 | admin 改商品价格 → portal 再查详情立即新值（缓存已失效） |

### 性能验证（测试阶段）
- 商品详情接口：缓存命中时响应时间 vs 未命中时对比
- 购物车查询/变更：目标 <20ms（本地环境参考值，标注为估计）
- MySQL 读压：对比开启缓存前后 DB 查询量（日志/慢查统计）

## 说明
- 所有性能数字（80% 降读压、20ms）是 PRD 目标，验证结果需记录实际值，不预设达标
- 面试时能讲"我测过什么、结果多少"即可加分，数据真实比漂亮重要
