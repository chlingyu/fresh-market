package com.freshmarket.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 购物车操作请求DTO
 */
@Schema(description = "购物车操作请求")
public class CartItemRequest {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", example = "1", required = true)
    private Long productId;

    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量必须大于0")
    @Schema(description = "商品数量", example = "2", required = true)
    private Integer quantity;

    // 默认构造函数
    public CartItemRequest() {}

    // 构造函数
    public CartItemRequest(Long productId, Integer quantity) {
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

    @Override
    public String toString() {
        return "CartItemRequest{" +
                "productId=" + productId +
                ", quantity=" + quantity +
                '}';
    }
}