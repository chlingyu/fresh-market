package com.freshmarket.payment.service;

import com.freshmarket.payment.dto.CreatePaymentRequest;
import com.freshmarket.payment.dto.PaymentCallbackRequest;
import com.freshmarket.payment.dto.PaymentResponse;
import com.freshmarket.payment.dto.PaymentStatusResponse;
import com.freshmarket.payment.entity.Payment;
import com.freshmarket.payment.enums.PaymentStatus;

import java.util.List;
import java.util.Optional;

/**
 * 支付服务接口
 * 定义了支付系统的核心业务逻辑
 * 
 * @author Fresh Market Team
 */
public interface PaymentService {

    /**
     * 创建支付请求
     * 根据订单信息创建支付记录，并返回支付链接或支付信息
     * 
     * @param request 包含订单ID和支付网关等信息的创建支付请求
     * @return PaymentResponse 包含支付状态和重定向信息等
     * @throws IllegalArgumentException 当订单不存在或订单状态不允许支付时
     * @throws IllegalStateException 当订单已有成功的支付记录时
     */
    PaymentResponse createPayment(CreatePaymentRequest request);

    /**
     * 处理支付网关的异步回调
     * 接收并处理第三方支付平台的支付结果通知
     * 
     * @param callbackData 支付回调数据，包含支付状态、交易流水号等信息
     * @return 处理结果，通常是一个简单的确认响应字符串（如"success"或"fail"）
     * @throws IllegalArgumentException 当回调数据无效时
     */
    String handlePaymentCallback(PaymentCallbackRequest callbackData);

    /**
     * 根据订单ID查询支付状态
     * 查询指定订单的最新支付状态信息
     * 
     * @param orderId 订单ID
     * @return PaymentStatusResponse 包含当前支付状态的响应对象
     * @throws IllegalArgumentException 当订单ID无效时
     */
    PaymentStatusResponse getPaymentStatusByOrderId(Long orderId);

    /**
     * 根据支付单号查询支付状态
     * 通过支付单号查询支付记录的详细状态
     * 
     * @param paymentNumber 支付单号
     * @return PaymentStatusResponse 包含当前支付状态的响应对象
     * @throws IllegalArgumentException 当支付单号无效时
     */
    PaymentStatusResponse getPaymentStatusByPaymentNumber(String paymentNumber);

    /**
     * 取消支付
     * 取消指定的支付请求（仅限PENDING或PROCESSING状态）
     * 
     * @param paymentId 支付记录ID
     * @return 是否取消成功
     * @throws IllegalArgumentException 当支付记录不存在时
     * @throws IllegalStateException 当支付状态不允许取消时
     */
    boolean cancelPayment(Long paymentId);

    /**
     * 查询订单的所有支付记录
     * 获取指定订单的所有支付尝试记录
     * 
     * @param orderId 订单ID
     * @return 支付记录列表，按创建时间倒序排列
     */
    List<PaymentResponse> getPaymentHistoryByOrderId(Long orderId);

    /**
     * 根据支付ID查询支付详情
     * 获取指定支付记录的详细信息
     * 
     * @param paymentId 支付记录ID
     * @return 支付记录详情，如果不存在则返回空
     */
    Optional<PaymentResponse> getPaymentById(Long paymentId);

    /**
     * 处理过期支付
     * 将已过期的待支付记录状态更新为CANCELLED
     * 此方法通常由定时任务调用
     * 
     * @return 处理的过期支付记录数量
     */
    int processExpiredPayments();

    /**
     * 验证支付回调签名
     * 验证第三方支付平台回调数据的签名是否有效
     * 
     * @param callbackData 回调数据
     * @return 签名是否有效
     */
    boolean verifyCallbackSignature(PaymentCallbackRequest callbackData);

    /**
     * 同步支付状态
     * 主动向第三方支付平台查询支付状态并更新本地记录
     * 
     * @param paymentId 支付记录ID
     * @return 更新后的支付记录
     * @throws IllegalArgumentException 当支付记录不存在时
     */
    PaymentResponse syncPaymentStatus(Long paymentId);
}