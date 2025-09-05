package com.freshmarket.payment.repository;

import com.freshmarket.payment.entity.Payment;
import com.freshmarket.payment.enums.PaymentGateway;
import com.freshmarket.payment.enums.PaymentStatus;
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
 * 支付记录数据访问层
 * 
 * @author Fresh Market Team
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 根据订单ID查询支付记录
     * @param orderId 订单ID
     * @return 支付记录列表
     */
    List<Payment> findByOrderId(Long orderId);

    /**
     * 根据订单ID查询最新的支付记录
     * @param orderId 订单ID
     * @return 最新的支付记录
     */
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId ORDER BY p.createdAt DESC")
    Optional<Payment> findLatestByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据支付单号查询支付记录
     * @param paymentNumber 支付单号
     * @return 支付记录
     */
    Optional<Payment> findByPaymentNumber(String paymentNumber);

    /**
     * 根据第三方交易流水号查询支付记录
     * @param transactionId 交易流水号
     * @return 支付记录
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * 根据支付状态查询支付记录
     * @param status 支付状态
     * @param pageable 分页参数
     * @return 支付记录分页结果
     */
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    /**
     * 根据支付网关查询支付记录
     * @param gateway 支付网关
     * @param pageable 分页参数
     * @return 支付记录分页结果
     */
    Page<Payment> findByGateway(PaymentGateway gateway, Pageable pageable);

    /**
     * 根据状态和网关查询支付记录
     * @param status 支付状态
     * @param gateway 支付网关
     * @param pageable 分页参数
     * @return 支付记录分页结果
     */
    Page<Payment> findByStatusAndGateway(PaymentStatus status, PaymentGateway gateway, Pageable pageable);

    /**
     * 查询过期的待支付记录
     * @param now 当前时间
     * @return 过期的支付记录列表
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.expiresAt < :now")
    List<Payment> findExpiredPayments(@Param("status") PaymentStatus status, @Param("now") Instant now);

    /**
     * 根据时间范围查询支付记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 支付记录分页结果
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startTime AND :endTime")
    Page<Payment> findByDateRange(@Param("startTime") Instant startTime, 
                                  @Param("endTime") Instant endTime, 
                                  Pageable pageable);

    /**
     * 根据订单ID和状态查询支付记录
     * @param orderId 订单ID
     * @param status 支付状态
     * @return 支付记录
     */
    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    /**
     * 统计指定状态的支付记录数量
     * @param status 支付状态
     * @return 记录数量
     */
    long countByStatus(PaymentStatus status);

    /**
     * 统计指定网关的支付记录数量
     * @param gateway 支付网关
     * @return 记录数量
     */
    long countByGateway(PaymentGateway gateway);

    /**
     * 统计指定时间范围内成功的支付总金额
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 支付总金额
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = 'SUCCESS' AND p.paidAt BETWEEN :startTime AND :endTime")
    Double sumSuccessfulPaymentAmount(@Param("startTime") Instant startTime, 
                                     @Param("endTime") Instant endTime);

    /**
     * 查询指定订单ID的所有支付记录，按创建时间倒序
     * @param orderId 订单ID
     * @return 支付记录列表
     */
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId ORDER BY p.createdAt DESC")
    List<Payment> findAllByOrderIdOrderByCreatedAtDesc(@Param("orderId") Long orderId);
}