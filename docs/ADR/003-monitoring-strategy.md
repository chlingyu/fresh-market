# ADR-003: 监控方案选择

## 状态
已采纳 (2024-12-05)

## 背景
生鲜电商系统需要在生产环境中具备问题发现、定位和解决的能力。作为个人开发的MVP项目，需要在监控完备性和运维复杂度之间找到平衡点。

核心监控需求：
- **性能监控**: API响应时间、吞吐量、错误率
- **业务监控**: 订单创建成功率、库存操作监控
- **系统监控**: CPU、内存、数据库连接池状态
- **问题排查**: 能够快速定位异步任务处理问题

## 决策
选择**日志聚合 + Prometheus指标收集**的简化监控方案，暂不引入分布式链路追踪(Jaeger/Zipkin)。

## 考虑的备选方案

### 方案A: 完整可观测性(三大支柱)
- **组件**: Logs(ELK) + Metrics(Prometheus) + Traces(Jaeger)
- **优势**: 问题排查能力最强，业界标准
- **劣势**: 运维复杂度极高，学习成本大，资源消耗重
- **适用场景**: 大规模微服务架构

### 方案B: 云厂商一体化方案
- **组件**: 阿里云ARMS、AWS X-Ray等
- **优势**: 开箱即用，运维简单
- **劣势**: 供应商绑定，成本高，学习意义有限
- **适用场景**: 商业项目快速上线

### 方案C: 轻量化自建方案(选中)
- **组件**: 结构化日志 + Prometheus + Grafana
- **优势**: 学习成本可控，运维简单，成本低
- **劣势**: 复杂问题排查能力有限
- **适用场景**: 中小规模单体应用

## 决策依据

### 项目约束分析
- **业务复杂度**: 用户→商品→订单，3步核心流程
- **异步链路**: 最长4步任务处理链(订单创建→库存扣减→价格计算→通知)
- **开发资源**: 1人开发，无专业运维
- **部署要求**: 20分钟启动，最小化外部依赖

### 成本-收益评估
- **Jaeger学习成本**: 2周深入理解 + 1周集成调试
- **运维复杂度**: 新增3个组件(Jaeger Agent/Collector/Query)
- **问题定位收益**: 在3步业务流程中收益有限
- **替代方案**: 统一TraceId贯穿日志 + 业务状态机追踪

## 实现设计

### 监控架构
```
Application Logs → Logback → File → (Optional: Filebeat → ELK)
Application Metrics → Micrometer → Prometheus → Grafana
Business Events → Custom Metrics → Prometheus
```

### 核心指标定义

#### 系统指标
```yaml
# JVM指标
jvm_memory_used_bytes          # 内存使用量
jvm_gc_pause_seconds           # GC停顿时间
jvm_threads_live_threads       # 活跃线程数

# 应用指标  
http_server_requests_seconds   # API响应时间分布
http_server_requests_total     # API请求总数
database_connections_active    # 数据库活跃连接数
```

#### 业务指标
```yaml
# 订单业务
order_created_total            # 订单创建总数
order_creation_duration_seconds # 订单创建耗时
order_payment_success_total    # 支付成功总数

# 任务队列
task_queue_pending_total       # 待处理任务数
task_execution_duration_seconds # 任务执行耗时
task_retry_total              # 任务重试次数
```

### 日志规范

#### 结构化日志格式
```json
{
  "timestamp": "2024-12-05T10:30:00Z",
  "level": "INFO",
  "traceId": "abc123def456",
  "spanId": "span789",
  "service": "fresh-market-api",
  "class": "OrderService", 
  "method": "createOrder",
  "message": "Order created successfully",
  "orderId": "ORD123456",
  "userId": "USER789",
  "amount": 99.99,
  "duration": 145
}
```

#### TraceId生成策略
```java
// 使用MDC在整个请求链路中传递TraceId
@Component
public class TraceContextFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

### 告警规则设计

#### 基于压测基线的阈值
```yaml
# 性能告警
- alert: HighResponseTime
  expr: histogram_quantile(0.99, http_server_requests_seconds) > 0.5
  for: 2m
  
- alert: HighOrderCreationLatency  
  expr: histogram_quantile(0.99, order_creation_duration_seconds) > 1.0
  for: 2m

# 业务告警  
- alert: OrderCreationFailureRate
  expr: rate(order_creation_failure_total[5m]) / rate(order_created_total[5m]) > 0.05
  for: 1m

# 系统资源告警
- alert: HighDatabaseConnectionUsage
  expr: database_connections_active / database_connections_max > 0.8
  for: 1m
```

## 后果

### 积极后果
✅ **运维简单**: 只需掌握Prometheus+Grafana，学习成本1周  
✅ **资源轻量**: 监控组件内存占用<1GB  
✅ **快速部署**: docker-compose一键启动包含监控  
✅ **核心覆盖**: 覆盖90%常见问题排查需求  

### 消极后果
❌ **链路追踪缺失**: 复杂异步问题排查困难  
❌ **日志关联性**: 需要手动通过TraceId关联日志  
❌ **细粒度缺失**: 无法精确定位慢查询SQL  

### 风险缓解
⚠️ **日志设计**: 关键业务节点记录详细日志  
⚠️ **状态机追踪**: 订单状态变更全程记录  
⚠️ **性能分析**: 集成Spring Boot Actuator性能端点  

## 问题排查策略

### 典型问题排查流程

#### 场景1: 订单创建响应慢
1. **Grafana看板**: 确认P99响应时间异常
2. **业务指标**: 检查order_creation_duration_seconds分布
3. **系统指标**: 查看数据库连接池、CPU使用率
4. **应用日志**: 根据TraceId搜索完整请求链路
5. **定位瓶颈**: DB慢查询 or 任务队列积压

#### 场景2: 异步任务处理失败
1. **任务队列指标**: 查看task_retry_total增长
2. **错误日志**: 搜索ERROR级别 + 具体taskType
3. **业务状态**: 检查订单状态是否卡在中间态
4. **补偿机制**: 手动触发任务重试或数据修复

### 日志搜索最佳实践
```bash
# 根据TraceId查询完整请求链路
grep "traceId=abc123def456" /var/log/fresh-market.log

# 查询特定订单的所有操作记录
grep "orderId=ORD123456" /var/log/fresh-market.log

# 查询任务执行异常
grep "task.*ERROR.*retry" /var/log/fresh-market.log
```

## 演进触发条件
当满足以下条件时，考虑引入分布式链路追踪:
1. 异步任务链路 > 5步
2. 服务拆分为 > 3个微服务
3. 复杂问题排查时间 > 2小时/次
4. 团队具备分布式追踪运维能力

## 监控成本预算
- **开发时间**: 监控集成1周
- **运行资源**: Prometheus(512MB) + Grafana(256MB)  
- **存储成本**: 指标数据30天保留，约100MB
- **学习成本**: Prometheus查询语言1周

## 参考资料
- [Prometheus Best Practices](https://prometheus.io/docs/practices/)
- [Spring Boot Actuator Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Grafana Dashboard Design](https://grafana.com/docs/grafana/latest/best-practices/)