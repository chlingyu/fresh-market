package com.freshmarket.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 用户地址响应DTO
 */
@Schema(description = "用户地址信息")
public class AddressResponse {

    @Schema(description = "地址ID", example = "1")
    private Long id;

    @Schema(description = "收货人姓名", example = "张三")
    private String name;

    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    @Schema(description = "详细地址", example = "北京市朝阳区xxx街道xxx号")
    private String address;

    @Schema(description = "是否为默认地址", example = "false")
    private Boolean isDefault;

    @Schema(description = "创建时间")
    private Instant createdAt;

    // 默认构造函数
    public AddressResponse() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AddressResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", isDefault=" + isDefault +
                ", createdAt=" + createdAt +
                '}';
    }
}