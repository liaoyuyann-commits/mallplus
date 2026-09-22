# Tech Stack — 技术栈与命令

> 来源：TechDesign-MallPlus-MVP.md。版本号标注"安装时验证"的，一律以官方文档当前版本为准，不要凭记忆写死。

## 技术栈

| 层 | 选型 | 说明 |
|---|---|---|
| 语言 | **Java 17** | 本机需从 JRE 8 升级（第 1 周装 Temurin JDK 17） |
| 后端框架 | **Spring Boot 3.x** + **Spring Cloud Alibaba** | 沿用原版，不改版本 |
| 前端 | 复用 mall 原版（mall-admin-web / mall-app） | 本阶段不开发前端 |
| 数据库 | **MySQL**（原版，`document/sql/mall.sql`） | 不加表不改表 |
| 缓存 | **Redis**（原版）+ **Caffeine 3.x**（新增） | Redis 已有 `RedisService` 封装可复用 |
| 布隆过滤器 | **Guava 33.x**（新增） | 单实例内存；多实例讲 Redisson |
| 消息队列 | **RocketMQ 4.x**（新增，真引入） | topic=`MALL_CACHE`；tag=`PRODUCT_EVICT`/`CART_MERGE`；不动原版 RabbitMQ |
| 鉴权 | Sa-Token（原版沿用） | 登录态在 Sa-Token Session |
| 构建 | Maven 3.9（需装） | 多模块：`mall-common/mbg/portal/admin/gateway/auth/...` |

## 关键依赖（pom.xml 待加）

- `mall-common`: `com.github.ben-manes.caffeine:caffeine:3.x`、`com.google.guava:guava:33.x`
- `mall-portal` + `mall-admin`: `org.apache.rocketmq:rocketmq-spring-boot-starter:2.3.x`（**安装时验证与 Spring Boot 3 兼容版本**）

## 命令

```bash
# 编译（第 1 周装好 JDK17+Maven 后）
mvn compile -pl mall-portal -am
mvn compile -pl mall-admin -am

# 打包
mvn package -pl mall-portal -am -DskipTests

# 启动（需 Nacos/MySQL/Redis 先行）
mvn spring-boot:run -pl mall-portal

# 测试
mvn test -pl mall-portal
```

## 关键配置文件位置

- `mall-portal/src/main/resources/application.yml`（portal 配置，含 redis.expire 等）
- `mall-admin/src/main/resources/application.yml`
- 新增配置：RocketMQ nameserver 地址、Caffeine 缓存参数（写入 application.yml，放 `mall-plus:` 自定义前缀下）

## 运行依赖的中间件（第 1 周）

Nacos（注册/配置中心，必须）· MySQL · Redis · RocketMQ（nameserver + broker，broker 内存调至 512M-1G）· RabbitMQ（测订单时）· Elasticsearch（测搜索时）· MongoDB（可跳过）

> 本机 C 盘仅 50G：中间件建议放云服务器（2C4G 40G）或本地精简启动（只起核心链路）。
