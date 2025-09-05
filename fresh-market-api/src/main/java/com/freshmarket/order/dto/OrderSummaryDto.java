package com.freshmarket.order.dto;

import com.freshmarket.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 订单摘要DTO - 用于列表查询的性能优化
 */
@Schema(description = "订单摘要信息")
public record OrderSummaryDto(
    @Schema(description = "订单ID", example = "1")
    Long id,
    
    @Schema(description = "订单编号", example = "ORD20240101001")
    String orderNumber,
    
    @Schema(description = "用户ID", example = "123")
    Long userId,
    
    @Schema(description = "订单状态", example = "PENDING")
    OrderStatus status,
    
    @Schema(description = "订单总金额", example = "99.99")
    BigDecimal totalAmount,
    
    @Schema(description = "创建时间")
    Instant createdAt
) {
    
    public OrderSummaryDto(Long id, String orderNumber, OrderStatus status, BigDecimal totalAmount) {
        this(id, orderNumber, null, status, totalAmount, null);
    }
    
    public OrderSummaryDto(Long id, String orderNumber, Long userId, OrderStatus status, BigDecimal totalAmount) {
        this(id, orderNumber, userId, status, totalAmount, null);
    }
}