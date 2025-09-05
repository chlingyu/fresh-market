package com.freshmarket.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 订单商品项请求DTO
 */
@Schema(description = "订单商品项请求")
public class OrderItemRequest {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", example = "1", required = true)
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    @Schema(description = "购买数量", example = "2", required = true)
    private Integer quantity;

    // 默认构造函数
    public OrderItemRequest() {}

    // 构造函数
    public OrderItemRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    // Getters and Setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}