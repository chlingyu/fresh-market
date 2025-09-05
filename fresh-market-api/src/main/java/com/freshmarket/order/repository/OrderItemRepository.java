package com.freshmarket.order.repository;

import com.freshmarket.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 订单商品明细数据访问层
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 根据订单ID查询订单商品列表
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 根据商品ID查询订单商品列表
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * 根据订单ID删除订单商品
     */
    void deleteByOrderId(Long orderId);

    /**
     * 查询商品销售统计
     */
    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity) as totalQuantity, SUM(oi.subtotal) as totalAmount " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.status IN ('PAID', 'SHIPPING', 'DELIVERED') " +
           "AND oi.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY oi.productId, oi.productName " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findProductSalesStatistics(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * 查询最受欢迎的商品
     */
    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity) as totalQuantity " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.status IN ('PAID', 'SHIPPING', 'DELIVERED') " +
           "GROUP BY oi.productId, oi.productName " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findPopularProducts(@Param("limit") int limit);

    /**
     * 计算商品总销售数量
     */
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi JOIN oi.order o WHERE oi.productId = :productId AND o.status IN ('PAID', 'SHIPPING', 'DELIVERED')")
    Long getTotalSoldQuantityByProductId(@Param("productId") Long productId);
}