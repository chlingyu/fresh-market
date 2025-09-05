package com.freshmarket.order.repository;

import com.freshmarket.order.entity.Order;
import com.freshmarket.order.entity.OrderItem;
import com.freshmarket.order.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单Repository测试
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("订单Repository测试")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Order pendingOrder;
    private Order paidOrder;
    private Order cancelledOrder;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        // 创建待支付订单
        pendingOrder = new Order(1L, "北京市朝阳区测试地址1", "13800138000");
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setNotes("测试订单1");
        OrderItem item1 = new OrderItem(1L, "测试商品1", new BigDecimal("10.00"), 2);
        pendingOrder.addOrderItem(item1);

        // 创建已支付订单
        paidOrder = new Order(1L, "北京市朝阳区测试地址2", "13800138001");
        paidOrder.setStatus(OrderStatus.PAID);
        paidOrder.setNotes("测试订单2");
        OrderItem item2 = new OrderItem(2L, "测试商品2", new BigDecimal("15.00"), 1);
        paidOrder.addOrderItem(item2);

        // 创建已取消订单 - 不同用户
        cancelledOrder = new Order(2L, "上海市浦东新区测试地址", "13800138002");
        cancelledOrder.setStatus(OrderStatus.CANCELLED);
        OrderItem item3 = new OrderItem(3L, "测试商品3", new BigDecimal("20.00"), 3);
        cancelledOrder.addOrderItem(item3);

        orderRepository.save(pendingOrder);
        orderRepository.save(paidOrder);
        orderRepository.save(cancelledOrder);
    }

    @Test
    @DisplayName("应该能根据用户ID查询订单")
    void shouldFindByUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orders = orderRepository.findByUserId(1L, pageable);

        assertThat(orders.getContent()).hasSize(2);
        assertThat(orders.getContent()).extracting(Order::getUserId).containsOnly(1L);
    }

    @Test
    @DisplayName("应该能根据用户ID和状态查询订单")
    void shouldFindByUserIdAndStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orders = orderRepository.findByUserIdAndStatus(1L, OrderStatus.PENDING, pageable);

        assertThat(orders.getContent()).hasSize(1);
        assertThat(orders.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("应该能根据订单编号查询订单")
    void shouldFindByOrderNumber() {
        String orderNumber = pendingOrder.getOrderNumber();
        Optional<Order> order = orderRepository.findByOrderNumber(orderNumber);

        assertThat(order).isPresent();
        assertThat(order.get().getId()).isEqualTo(pendingOrder.getId());
    }

    @Test
    @DisplayName("应该能根据用户ID和订单编号查询订单")
    void shouldFindByUserIdAndOrderNumber() {
        String orderNumber = paidOrder.getOrderNumber();
        Optional<Order> order = orderRepository.findByUserIdAndOrderNumber(1L, orderNumber);

        assertThat(order).isPresent();
        assertThat(order.get().getId()).isEqualTo(paidOrder.getId());
    }

    @Test
    @DisplayName("应该能根据用户ID和订单ID查询订单")
    void shouldFindByUserIdAndId() {
        Optional<Order> order = orderRepository.findByUserIdAndId(1L, pendingOrder.getId());

        assertThat(order).isPresent();
        assertThat(order.get().getId()).isEqualTo(pendingOrder.getId());

        // 测试不同用户无法查询
        Optional<Order> notFound = orderRepository.findByUserIdAndId(2L, pendingOrder.getId());
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("应该能根据时间范围查询订单")
    void shouldFindByCreatedAtBetween() {
        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now().plus(1, ChronoUnit.HOURS);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> orders = orderRepository.findByCreatedAtBetween(startTime, endTime, pageable);

        assertThat(orders.getContent()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("应该能根据用户ID和时间范围查询订单")
    void shouldFindByUserIdAndCreatedAtBetween() {
        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now().plus(1, ChronoUnit.HOURS);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> orders = orderRepository.findByUserIdAndCreatedAtBetween(1L, startTime, endTime, pageable);

        assertThat(orders.getContent()).hasSize(2);
        assertThat(orders.getContent()).extracting(Order::getUserId).containsOnly(1L);
    }

    @Test
    @DisplayName("应该能根据订单编号关键词模糊查询")
    void shouldFindByOrderNumberContainingIgnoreCase() {
        String keyword = "ORD";
        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> orders = orderRepository.findByOrderNumberContainingIgnoreCase(keyword, pageable);

        assertThat(orders.getContent()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(orders.getContent()).allMatch(order -> 
                order.getOrderNumber().toUpperCase().contains(keyword.toUpperCase()));
    }

    @Test
    @DisplayName("应该能复合条件查询订单")
    void shouldFindByComplexConditions() {
        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now().plus(1, ChronoUnit.HOURS);
        Pageable pageable = PageRequest.of(0, 10);

        // 测试用户ID + 状态条件
        Page<Order> result1 = orderRepository.findByComplexConditions(
                1L, OrderStatus.PENDING, null, null, null, pageable);
        assertThat(result1.getContent()).hasSize(1);

        // 测试时间范围条件
        Page<Order> result2 = orderRepository.findByComplexConditions(
                null, null, startTime, endTime, null, pageable);
        assertThat(result2.getContent()).hasSizeGreaterThanOrEqualTo(3);

        // 测试订单编号关键词
        Page<Order> result3 = orderRepository.findByComplexConditions(
                null, null, null, null, "ORD", pageable);
        assertThat(result3.getContent()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("应该能统计用户订单数量按状态")
    void shouldCountOrdersByUserIdAndStatus() {
        List<Object[]> statistics = orderRepository.countOrdersByUserIdAndStatus(1L);

        assertThat(statistics).hasSize(2); // PENDING 和 PAID 状态
        
        // 验证统计数据
        boolean hasPending = statistics.stream()
                .anyMatch(row -> row[0] == OrderStatus.PENDING && ((Long) row[1]) == 1);
        boolean hasPaid = statistics.stream()
                .anyMatch(row -> row[0] == OrderStatus.PAID && ((Long) row[1]) == 1);
        
        assertThat(hasPending).isTrue();
        assertThat(hasPaid).isTrue();
    }

    @Test
    @DisplayName("应该能查询指定状态的订单数量")
    void shouldCountByStatus() {
        long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);
        long paidCount = orderRepository.countByStatus(OrderStatus.PAID);
        long cancelledCount = orderRepository.countByStatus(OrderStatus.CANCELLED);

        assertThat(pendingCount).isEqualTo(1);
        assertThat(paidCount).isEqualTo(1);
        assertThat(cancelledCount).isEqualTo(1);
    }

    @Test
    @DisplayName("应该能查询指定用户和状态的订单数量")
    void shouldCountByUserIdAndStatus() {
        long user1PendingCount = orderRepository.countByUserIdAndStatus(1L, OrderStatus.PENDING);
        long user1PaidCount = orderRepository.countByUserIdAndStatus(1L, OrderStatus.PAID);
        long user2CancelledCount = orderRepository.countByUserIdAndStatus(2L, OrderStatus.CANCELLED);

        assertThat(user1PendingCount).isEqualTo(1);
        assertThat(user1PaidCount).isEqualTo(1);
        assertThat(user2CancelledCount).isEqualTo(1);
    }

    @Test
    @DisplayName("应该能查询指定时间段内的订单")
    void shouldFindRecentOrders() {
        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now().plus(1, ChronoUnit.HOURS);

        List<Order> orders = orderRepository.findRecentOrders(startTime, endTime);

        assertThat(orders).hasSizeGreaterThanOrEqualTo(3);
        assertThat(orders).allMatch(order -> 
                !order.getCreatedAt().isBefore(startTime) && 
                !order.getCreatedAt().isAfter(endTime));
    }

    @Test
    @DisplayName("应该能查询待支付超时订单")
    void shouldFindTimeoutPendingOrders() {
        // 创建一个过期的待支付订单
        Order timeoutOrder = new Order(3L, "超时订单地址", "13800138003");
        timeoutOrder.setStatus(OrderStatus.PENDING);
        // 手动设置创建时间为2小时前
        Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        orderRepository.save(timeoutOrder);
        
        // 直接更新创建时间
        entityManager.flush();
        entityManager.getEntityManager().createQuery(
                "UPDATE Order o SET o.createdAt = :createdAt WHERE o.id = :id")
                .setParameter("createdAt", twoHoursAgo)
                .setParameter("id", timeoutOrder.getId())
                .executeUpdate();
        entityManager.clear();

        Instant timeoutTime = Instant.now().minus(1, ChronoUnit.HOURS);
        List<Order> timeoutOrders = orderRepository.findTimeoutPendingOrders(timeoutTime);

        assertThat(timeoutOrders).hasSizeGreaterThanOrEqualTo(1);
        assertThat(timeoutOrders).allMatch(order -> 
                order.getStatus() == OrderStatus.PENDING && 
                order.getCreatedAt().isBefore(timeoutTime));
    }
}