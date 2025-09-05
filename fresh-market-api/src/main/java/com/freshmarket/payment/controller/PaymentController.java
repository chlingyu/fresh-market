package com.freshmarket.payment.controller;

import com.freshmarket.common.dto.BaseResponse;
import com.freshmarket.payment.dto.CreatePaymentRequest;
import com.freshmarket.payment.dto.PaymentCallbackRequest;
import com.freshmarket.payment.dto.PaymentResponse;
import com.freshmarket.payment.dto.PaymentStatusResponse;
import com.freshmarket.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 支付控制器
 * 处理支付相关的REST API请求
 * 
 * @author Fresh Market Team
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "支付管理", description = "支付创建、状态查询、回调处理等相关API")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}/pay")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "创建支付请求",
        description = "为指定订单创建支付请求，需要用户登录",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "201", description = "支付请求创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误或订单状态不允许支付"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "订单不存在"),
            @ApiResponse(responseCode = "409", description = "订单已有成功的支付记录")
        }
    )
    public BaseResponse<PaymentResponse> createPayment(
            Authentication authentication,
            @Parameter(description = "订单ID", required = true)
            @PathVariable Long orderId,
            @Valid @RequestBody CreatePaymentRequest request) {
        
        logger.info("Creating payment for order: {} with method: {}", orderId, request.getPaymentMethod());
        
        // 设置订单ID（从路径参数）
        request.setOrderId(orderId);
        
        PaymentResponse payment = paymentService.createPayment(request);
        return BaseResponse.success(payment);
    }

    @GetMapping("/orders/{orderId}/status")
    @Operation(
        summary = "查询订单支付状态",
        description = "获取指定订单的最新支付状态",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "订单或支付记录不存在")
        }
    )
    public BaseResponse<PaymentStatusResponse> getOrderPaymentStatus(
            Authentication authentication,
            @Parameter(description = "订单ID", required = true)
            @PathVariable Long orderId) {
        
        logger.debug("Getting payment status for order: {}", orderId);
        
        PaymentStatusResponse status = paymentService.getPaymentStatusByOrderId(orderId);
        return BaseResponse.success(status);
    }

    @GetMapping("/{paymentNumber}/status")
    @Operation(
        summary = "根据支付单号查询支付状态",
        description = "通过支付单号获取支付状态详情",
        responses = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "支付记录不存在")
        }
    )
    public BaseResponse<PaymentStatusResponse> getPaymentStatus(
            @Parameter(description = "支付单号", required = true)
            @PathVariable String paymentNumber) {
        
        logger.debug("Getting payment status for payment number: {}", paymentNumber);
        
        PaymentStatusResponse status = paymentService.getPaymentStatusByPaymentNumber(paymentNumber);
        return BaseResponse.success(status);
    }

    @PostMapping("/callback")
    @Operation(
        summary = "处理支付回调",
        description = "接收第三方支付平台的异步回调通知",
        responses = {
            @ApiResponse(responseCode = "200", description = "回调处理成功"),
            @ApiResponse(responseCode = "400", description = "回调数据无效")
        }
    )
    public String handlePaymentCallback(@Valid @RequestBody PaymentCallbackRequest callbackData) {
        logger.info("Received payment callback for payment number: {}", callbackData.getPaymentNumber());
        
        try {
            String result = paymentService.handlePaymentCallback(callbackData);
            logger.info("Payment callback processed successfully, result: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Failed to process payment callback", e);
            return "fail";
        }
    }

    @GetMapping("/orders/{orderId}/history")
    @Operation(
        summary = "查询订单支付历史",
        description = "获取指定订单的所有支付尝试记录",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<List<PaymentResponse>> getOrderPaymentHistory(
            Authentication authentication,
            @Parameter(description = "订单ID", required = true)
            @PathVariable Long orderId) {
        
        logger.debug("Getting payment history for order: {}", orderId);
        
        List<PaymentResponse> history = paymentService.getPaymentHistoryByOrderId(orderId);
        return BaseResponse.success(history);
    }

    @GetMapping("/{paymentId}")
    @Operation(
        summary = "获取支付详情",
        description = "根据支付ID获取支付记录详情",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "支付记录不存在")
        }
    )
    public BaseResponse<PaymentResponse> getPaymentById(
            Authentication authentication,
            @Parameter(description = "支付ID", required = true)
            @PathVariable Long paymentId) {
        
        logger.debug("Getting payment details for payment ID: {}", paymentId);
        
        Optional<PaymentResponse> payment = paymentService.getPaymentById(paymentId);
        if (payment.isPresent()) {
            return BaseResponse.success(payment.get());
        } else {
            return BaseResponse.error("NOT_FOUND", "Payment not found");
        }
    }

    @PutMapping("/{paymentId}/cancel")
    @Operation(
        summary = "取消支付",
        description = "取消指定的支付请求（仅限PENDING或PROCESSING状态）",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "400", description = "支付状态不允许取消"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "支付记录不存在")
        }
    )
    public BaseResponse<String> cancelPayment(
            Authentication authentication,
            @Parameter(description = "支付ID", required = true)
            @PathVariable Long paymentId) {
        
        logger.info("Cancelling payment: {}", paymentId);
        
        boolean cancelled = paymentService.cancelPayment(paymentId);
        if (cancelled) {
            return BaseResponse.success("Payment cancelled successfully");
        } else {
            return BaseResponse.error("BAD_REQUEST", "Failed to cancel payment");
        }
    }

    @PostMapping("/{paymentId}/sync")
    @Operation(
        summary = "同步支付状态",
        description = "主动向第三方支付平台查询支付状态并更新本地记录",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "同步成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "支付记录不存在")
        }
    )
    public BaseResponse<PaymentResponse> syncPaymentStatus(
            Authentication authentication,
            @Parameter(description = "支付ID", required = true)
            @PathVariable Long paymentId) {
        
        logger.info("Syncing payment status for payment: {}", paymentId);
        
        PaymentResponse payment = paymentService.syncPaymentStatus(paymentId);
        return BaseResponse.success(payment);
    }

    /**
     * 从认证信息中提取用户ID
     * 这里是简化实现，实际项目中会根据JWT或其他认证方式提取用户信息
     */
    private Long getUserId(Authentication authentication) {
        // 简化实现，实际中应该从JWT中解析用户ID
        return 1L; // 临时返回固定用户ID
    }
}