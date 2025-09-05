package com.freshmarket.order.dto;

import com.freshmarket.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 订单响应DTO
 */
@Schema(description = "订单信息响应")
public class OrderResponse {

    @Schema(description = "订单ID", example = "1")
    private Long id;

    @Schema(description = "订单编号", example = "ORD16912345678901234")
    private String orderNumber;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "订单状态", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "订单总金额", example = "99.99")
    private BigDecimal totalAmount;

    @Schema(description = "配送地址", example = "北京市朝阳区xx街道xx号")
    private String shippingAddress;

    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    @Schema(description = "订单备注", example = "请在工作日配送")
    private String notes;

    @Schema(description = "创建时间")
    private Instant createdAt;

    @Schema(description = "更新时间")
    private Instant updatedAt;

    @Schema(description = "订单商品列表")
    private List<OrderItemResponse> orderItems;

    // 默认构造函数
    public OrderResponse() {}

    // 构造函数
    public OrderResponse(Long id, String orderNumber, OrderStatus status, BigDecimal totalAmount) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public List<OrderItemResponse> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemResponse> orderItems) {
        this.orderItems = orderItems;
    }
}