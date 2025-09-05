package com.freshmarket.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车汇总响应DTO
 */
@Schema(description = "购物车汇总信息")
public class CartSummaryResponse {

    @Schema(description = "购物车商品列表")
    private List<CartItemResponse> items;

    @Schema(description = "商品总数量", example = "5")
    private Integer totalQuantity;

    @Schema(description = "商品种类数", example = "3")
    private Integer totalItems;

    @Schema(description = "购物车总金额", example = "128.50")
    private BigDecimal totalAmount;

    @Schema(description = "可用商品数量", example = "4")
    private Integer availableItems;

    @Schema(description = "不可用商品数量", example = "1")
    private Integer unavailableItems;

    // 默认构造函数
    public CartSummaryResponse() {}

    // 构造函数
    public CartSummaryResponse(List<CartItemResponse> items) {
        this.items = items;
        calculateSummary();
    }

    /**
     * 计算购物车汇总信息
     */
    private void calculateSummary() {
        if (items == null || items.isEmpty()) {
            this.totalQuantity = 0;
            this.totalItems = 0;
            this.totalAmount = BigDecimal.ZERO;
            this.availableItems = 0;
            this.unavailableItems = 0;
            return;
        }

        this.totalItems = items.size();
        this.totalQuantity = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        this.totalAmount = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.availableItems = Math.toIntExact(items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                .count());

        this.unavailableItems = this.totalItems - this.availableItems;
    }

    // Getters and Setters
    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
        calculateSummary();
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getAvailableItems() {
        return availableItems;
    }

    public void setAvailableItems(Integer availableItems) {
        this.availableItems = availableItems;
    }

    public Integer getUnavailableItems() {
        return unavailableItems;
    }

    public void setUnavailableItems(Integer unavailableItems) {
        this.unavailableItems = unavailableItems;
    }

    @Override
    public String toString() {
        return "CartSummaryResponse{" +
                "totalQuantity=" + totalQuantity +
                ", totalItems=" + totalItems +
                ", totalAmount=" + totalAmount +
                ", availableItems=" + availableItems +
                ", unavailableItems=" + unavailableItems +
                '}';
    }
}