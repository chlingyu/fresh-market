package com.freshmarket.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建订单请求DTO
 */
@Schema(description = "创建订单请求")
public class CreateOrderRequest {

    @NotEmpty(message = "订单商品列表不能为空")
    @Valid
    @Schema(description = "订单商品列表", required = true)
    private List<OrderItemRequest> orderItems;

    @NotBlank(message = "配送地址不能为空")
    @Size(max = 200, message = "配送地址长度不能超过200个字符")
    @Schema(description = "配送地址", example = "北京市朝阳区xx街道xx号", required = true)
    private String shippingAddress;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "联系电话", example = "13800138000", required = true)
    private String phone;

    @Size(max = 500, message = "订单备注长度不能超过500个字符")
    @Schema(description = "订单备注", example = "请在工作日配送")
    private String notes;

    // 默认构造函数
    public CreateOrderRequest() {}

    // Getters and Setters
    public List<OrderItemRequest> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemRequest> orderItems) {
        this.orderItems = orderItems;
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
}