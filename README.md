# Fresh Market - 生鲜电商系统

## 项目愿景

Fresh Market是一个专门为**技术展示和求职面试**设计的生鲜电商MVP系统。核心目标是让面试官能在**20分钟内完成本地部署**并演示完整的用户购买流程，展示现代Java后端开发的工程实践能力。

## 快速开始 🚀

### 一键部署
```bash
# 克隆项目
git clone https://github.com/your-username/fresh-market.git
cd fresh-market

# 启动系统(包含MySQL、Redis、应用)
docker-compose up -d

# 等待服务启动完成(约2分钟)
docker-compose logs -f fresh-market-api

# 访问应用
open http://localhost:8080
```

### 验证部署
```bash
# 检查服务状态
curl http://localhost:8080/actuator/health

# 预置数据已自动创建，可立即演示完整流程
```

## 核心功能 ✨

### MVP范围(已实现)
- ✅ **用户模块**: 注册/登录(JWT认证)、个人信息管理
- ✅ **商品模块**: 商品列表/详情、分类筛选、库存管理  
- ✅ **购物车模块**: 商品增删改、购物车同步
- ✅ **订单模块**: 订单创建/查询、状态管理
- ✅ **支付模块**: 模拟支付流程
- ✅ **后台管理**: 商品CRUD、库存调整

### 演示流程
1. **用户注册**: POST /api/v1/users/register
2. **浏览商品**: GET /api/v1/products  
3. **加入购物车**: POST /api/v1/cart/items
4. **创建订单**: POST /api/v1/orders
5. **完成支付**: POST /api/v1/orders/{id}/pay

## 技术架构 🏗️

### 架构设计图
```
[用户] → [Nginx] → [Spring Boot App] → [MySQL]
                           ↓              ↑
                      [Redis Cache] → [Task Queue]
```

### 技术栈
| 分层 | 技术选择 | 版本 | 说明 |
|-----|---------|------|------|
| **框架** | Spring Boot | 3.2.0 | 主框架 |
| **数据库** | MySQL | 8.0 | 主数据存储 |
| **缓存** | Redis | 7.0 | 商品缓存、会话管理 |  
| **容器化** | Docker Compose | - | 一键部署 |
| **监控** | Prometheus + Grafana | - | 指标收集和展示 |
| **文档** | OpenAPI 3.0 | - | API文档自动生成 |

### 核心设计决策

本项目的技术选择基于明确的约束条件和权衡分析，详见架构决策记录(ADR)：

- **[ADR-001: 数据库任务队列](./docs/ADR/001-database-task-queue.md)**: 为什么选择数据库任务表而不是RabbitMQ
- **[ADR-002: 缓存一致性策略](./docs/ADR/002-cache-consistency-strategy.md)**: 为什么选择双写策略而不是CDC
- **[ADR-003: 监控方案](./docs/ADR/003-monitoring-strategy.md)**: 为什么暂不引入分布式链路追踪

## 项目结构 📁

```
fresh-market/
├── docs/                   # 项目文档
│   ├── ADR/               # 架构决策记录  
│   └── api/               # API规范文档
├── fresh-market-common/    # 公共组件
├── fresh-market-api/       # 主应用
│   ├── src/main/java/com/freshmarket/
│   │   ├── user/          # 用户模块
│   │   ├── product/       # 商品模块  
│   │   ├── order/         # 订单模块
│   │   ├── cart/          # 购物车模块
│   │   └── task/          # 异步任务模块
│   └── src/test/          # 测试代码
├── docker-compose.yml     # 容器编排  
├── prometheus.yml         # 监控配置
└── README.md              # 项目说明
```

## API文档 📖

### 在线文档
启动应用后访问: http://localhost:8080/swagger-ui.html

### 核心API端点

#### 用户模块
```http
POST   /api/v1/users/register     # 用户注册
POST   /api/v1/users/login        # 用户登录  
GET    /api/v1/users/profile      # 获取用户信息
```

#### 商品模块  
```http
GET    /api/v1/products           # 商品列表(支持分页、筛选)
GET    /api/v1/products/{id}      # 商品详情
GET    /api/v1/categories         # 商品分类
```

#### 订单模块
```http
POST   /api/v1/orders             # 创建订单
GET    /api/v1/orders/{id}        # 获取订单详情  
GET    /api/v1/orders             # 用户订单列表
POST   /api/v1/orders/{id}/pay    # 支付订单
```

## 监控与运维 📊

### 监控面板
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **应用健康检查**: http://localhost:8080/actuator/health

### 关键指标
- **性能指标**: API响应时间P99、QPS、错误率
- **业务指标**: 订单创建成功率、支付成功率
- **系统指标**: JVM内存、数据库连接池、任务队列长度

### 日志查看
```bash
# 实时日志
docker-compose logs -f fresh-market-api

# 错误日志筛选
docker-compose logs fresh-market-api | grep ERROR

# 特定订单追踪(通过TraceId)
docker-compose exec fresh-market-api grep "traceId=abc123" /var/log/app.log
```

## 开发指南 👨‍💻

### 本地开发环境
```bash
# 启动依赖服务
docker-compose up -d mysql redis

# IDE中启动Spring Boot应用
mvn spring-boot:run -pl fresh-market-api

# 或者完整容器化开发
docker-compose up --build
```

### 测试
```bash
# 单元测试
mvn test

# 集成测试  
mvn verify -P integration-test

# 测试覆盖率报告
mvn jacoco:report
```

### 数据库管理
```bash
# 连接数据库
docker-compose exec mysql mysql -u root -p fresh_market

# 数据迁移(Flyway)
mvn flyway:migrate -pl fresh-market-api
```

## 性能基准 🎯

基于4C8G服务器的压测结果：

| 并发用户 | 订单创建P99 | 商品查询P99 | CPU峰值 | 内存峰值 |
|---------|-------------|-------------|---------|----------|
| 50      | 150ms       | 30ms        | 45%     | 60%      |
| 100     | 180ms       | 35ms        | 55%     | 65%      |
| 150     | 250ms       | 50ms        | 70%     | 75%      |
| 200     | 450ms       | 80ms        | 75%     | 80%      |

**推荐配置**: 100并发用户为最佳性价比区间

## 演进规划 🛣️

### 已识别的技术债务
1. **任务队列扩展性**: 当实例数>5时需迁移到RabbitMQ
2. **缓存一致性**: 高频更新场景下考虑引入CDC
3. **监控完善**: 复杂业务场景需要分布式链路追踪

### 演进触发条件
详见各个ADR文档中的演进触发条件定义。

## 面试演示建议 🎤

### 演示流程 (15分钟)
1. **快速部署** (3分钟): docker-compose up演示  
2. **API测试** (5分钟): Postman演示完整用户流程
3. **架构解释** (4分钟): 展示ADR文档，说明关键技术选择
4. **监控展示** (3分钟): Grafana面板，展示系统指标

### 技术亮点
- ✨ **工程思维**: ADR文档体现的权衡思考
- ✨ **异步处理**: 数据库任务队列的可靠性设计  
- ✨ **缓存一致性**: 双写策略的实现和补偿机制
- ✨ **可观测性**: 结构化日志和业务指标监控
- ✨ **部署友好**: 一键启动的部署体验

## 贡献指南 🤝

### 开发规范
- 代码风格: 遵循Google Java Style
- 提交规范: 使用Conventional Commits
- 测试要求: 新功能需配套单元测试

### 提交流程
1. Fork项目
2. 创建功能分支: `git checkout -b feature/amazing-feature`
3. 提交更改: `git commit -m 'feat: add amazing feature'`
4. 推送分支: `git push origin feature/amazing-feature`
5. 提交PR

## 常见问题 ❓

### Q: 为什么不用微服务架构？
A: 见[ADR-001](./docs/ADR/001-database-task-queue.md)，基于个人开发约束的理性选择。

### Q: 生产环境如何部署？
A: 提供docker-compose生产配置，建议云服务器4C8G配置。

### Q: 如何扩展新功能？
A: 遵循现有模块划分，新增功能前请阅读ADR文档了解设计约束。

## 许可证 📄

MIT License - 详见 [LICENSE](LICENSE) 文件

---

**Built with ❤️ for Tech Interview Excellence**

> 这个项目不仅仅是一个电商系统，更是一次现代Java后端开发最佳实践的完整展示。
> 
> 从需求分析、架构设计、技术选型到工程实施，每一个环节都体现了工程师的权衡思考和专业素养。