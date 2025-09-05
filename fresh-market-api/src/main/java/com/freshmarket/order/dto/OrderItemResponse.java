package com.freshmarket.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 订单商品项响应DTO
 */
@Schema(description = "订单商品项信息响应")
public class OrderItemResponse {

    @Schema(description = "订单商品项ID", example = "1")
    private Long id;

    @Schema(description = "商品ID", example = "1")
    private Long productId;

    @Schema(description = "商品名称", example = "新鲜苹果")
    private String productName;

    @Schema(description = "商品单价", example = "12.50")
    private BigDecimal productPrice;

    @Schema(description = "购买数量", example = "2")
    private Integer quantity;

    @Schema(description = "小计金额", example = "25.00")
    private BigDecimal subtotal;

    @Schema(description = "创建时间")
    private Instant createdAt;

    // 默认构造函数
    public OrderItemResponse() {}

    // 构造函数
    public OrderItemResponse(Long id, Long productId, String productName, BigDecimal productPrice, Integer quantity, BigDecimal subtotal) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}