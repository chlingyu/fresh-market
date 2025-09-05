package com.freshmarket.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 购物车商品响应DTO
 */
@Schema(description = "购物车商品信息")
public class CartItemResponse {

    @Schema(description = "购物车项ID", example = "1")
    private Long id;

    @Schema(description = "商品ID", example = "1") 
    private Long productId;

    @Schema(description = "商品名称", example = "新鲜苹果")
    private String productName;

    @Schema(description = "商品价格", example = "12.50")
    private BigDecimal productPrice;

    @Schema(description = "商品图片URL", example = "https://example.com/apple.jpg")
    private String productImageUrl;

    @Schema(description = "商品数量", example = "2")
    private Integer quantity;

    @Schema(description = "小计金额", example = "25.00")
    private BigDecimal subtotal;

    @Schema(description = "商品是否可用", example = "true")
    private Boolean available;

    @Schema(description = "创建时间")
    private Instant createdAt;

    @Schema(description = "更新时间")
    private Instant updatedAt;

    // 默认构造函数
    public CartItemResponse() {}

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

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
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

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "CartItemResponse{" +
                "id=" + id +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", productPrice=" + productPrice +
                ", quantity=" + quantity +
                ", subtotal=" + subtotal +
                ", available=" + available +
                '}';
    }
}