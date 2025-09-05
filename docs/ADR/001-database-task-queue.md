# ADR-001: 异步任务处理方案选择

## 状态
已采纳 (2024-12-05)

## 背景
在生鲜电商系统中，订单创建后需要执行一系列异步操作：
- 库存扣减与同步
- 订单价格计算
- 用户通知发送
- 支付状态检查

这些操作不应阻塞用户订单创建的响应，需要一套可靠的异步任务处理机制。

## 决策
选择**数据库任务表 + 定时轮询**的方案，而不是Spring @Async或消息队列(RabbitMQ)。

## 考虑的备选方案

### 方案A: Spring @Async
- **优势**: 实现简单，零配置
- **劣势**: 任务易丢失(应用崩溃)，无法持久化，无重试机制
- **适用场景**: 非关键任务，如邮件发送

### 方案B: RabbitMQ消息队列
- **优势**: 高可靠性，成熟的队列机制，丰富的路由策略
- **劣势**: 增加外部依赖，运维复杂度高，学习成本大
- **适用场景**: 大规模分布式系统

### 方案C: 数据库任务表(选中)
- **优势**: 零外部依赖，事务一致性，简单部署
- **劣势**: 存在轮询开销，多实例惊群效应
- **适用场景**: 中小规模项目，快速MVP

## 决策依据

### 项目约束条件
- **开发人员**: 1人
- **开发周期**: 3个月
- **部署要求**: 20分钟一键启动
- **运维能力**: 有限
- **目标用户**: 面试官技术演示

### 量化分析
- **预期规模**: 2个应用实例，任务量<100/分钟
- **轮询开销**: 5秒/次 × 2实例 = 数据库额外负载<1%
- **乐观锁冲突**: 预期冲突率<5%，可接受
- **开发时间**: 数据库方案2天 vs RabbitMQ方案2周

## 实现设计

### 任务表结构
```sql
CREATE TABLE task_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type VARCHAR(50) NOT NULL,
    business_id VARCHAR(100) NOT NULL,
    payload JSON,
    status VARCHAR(20) DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    max_retry INT DEFAULT 3,
    next_retry_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 关键特性
- **持久化**: 任务存储在数据库中，应用崩溃不丢失
- **重试机制**: 支持指数退避重试
- **幂等性**: 通过业务ID防重复执行
- **监控**: 任务执行状态可追溯

## 后果

### 积极后果
✅ **零依赖部署**: `docker-compose up`即可启动全系统  
✅ **事务一致性**: 业务数据与任务创建在同一事务中  
✅ **简单运维**: 无需学习消息队列运维知识  
✅ **快速开发**: 符合3个月MVP交付目标  

### 消极后果  
❌ **惊群效应**: 多实例同时轮询，存在资源浪费  
❌ **扩展性限制**: 实例数增长时性能下降  
❌ **实时性**: 最大5秒延迟(轮询间隔)  

### 风险缓解
⚠️ **性能监控**: 监控任务查询P99延迟，设置100ms告警阈值  
⚠️ **演进路径**: 当P99>100ms或实例数>5时，迁移至RabbitMQ  
⚠️ **连接池隔离**: 为任务处理器预留独立连接池  

## 演进触发条件
当满足以下任一条件时，重新评估并迁移至RabbitMQ:
1. 任务查询P99延迟 > 100ms
2. 应用实例数 > 5个  
3. 任务量峰值 > 1000/分钟
4. 运维团队具备消息队列管理能力

## 参考资料
- [Database as Queue Anti-Pattern](https://www.cloudamqp.com/blog/2014-12-03-why-is-a-database-not-the-right-tool-for-a-queue-based-system.html)
- [Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Spring Boot @Async Best Practices](https://spring.io/guides/gs/async-method/)