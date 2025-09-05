package com.freshmarket.order.repository;

import com.freshmarket.order.entity.Order;
import com.freshmarket.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 订单数据访问层
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 根据用户ID查询订单列表
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户ID和状态查询订单列表
     */
    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    /**
     * 根据订单编号查询订单
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 根据用户ID和订单编号查询订单
     */
    Optional<Order> findByUserIdAndOrderNumber(Long userId, String orderNumber);

    /**
     * 根据用户ID和订单ID查询订单
     */
    Optional<Order> findByUserIdAndId(Long userId, Long orderId);

    /**
     * 根据时间范围查询订单
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startTime AND :endTime")
    Page<Order> findByCreatedAtBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime, Pageable pageable);

    /**
     * 根据用户ID和时间范围查询订单
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.createdAt BETWEEN :startTime AND :endTime")
    Page<Order> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime, Pageable pageable);

    /**
     * 根据订单编号关键词模糊查询
     */
    Page<Order> findByOrderNumberContainingIgnoreCase(String orderNumberKeyword, Pageable pageable);

    /**
     * 根据用户ID和订单编号关键词模糊查询
     */
    Page<Order> findByUserIdAndOrderNumberContainingIgnoreCase(Long userId, String orderNumberKeyword, Pageable pageable);

    /**
     * 复合条件查询订单
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(:userId IS NULL OR o.userId = :userId) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:startTime IS NULL OR o.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR o.createdAt <= :endTime) AND " +
           "(:orderNumberKeyword IS NULL OR UPPER(o.orderNumber) LIKE UPPER(CONCAT('%', :orderNumberKeyword, '%')))")
    Page<Order> findByComplexConditions(@Param("userId") Long userId,
                                      @Param("status") OrderStatus status,
                                      @Param("startTime") Instant startTime,
                                      @Param("endTime") Instant endTime,
                                      @Param("orderNumberKeyword") String orderNumberKeyword,
                                      Pageable pageable);

    /**
     * 统计用户订单数量按状态
     */
    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.userId = :userId GROUP BY o.status")
    List<Object[]> countOrdersByUserIdAndStatus(@Param("userId") Long userId);

    /**
     * 查询指定状态的订单数量
     */
    long countByStatus(OrderStatus status);

    /**
     * 查询指定用户和状态的订单数量
     */
    long countByUserIdAndStatus(Long userId, OrderStatus status);

    /**
     * 查询指定时间段内的订单
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :startTime AND o.createdAt <= :endTime ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * 查询待支付超时订单
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :timeoutTime")
    List<Order> findTimeoutPendingOrders(@Param("timeoutTime") Instant timeoutTime);
}