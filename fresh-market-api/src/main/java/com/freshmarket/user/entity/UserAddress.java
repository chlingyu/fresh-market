package com.freshmarket.user.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 用户地址实体
 */
@Entity
@Table(name = "user_addresses")
@Schema(description = "用户地址信息")
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "地址ID")
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    @Schema(description = "用户ID")
    private Long userId;

    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 50, message = "收货人姓名长度不能超过50个字符")
    @Column(nullable = false, length = 50)
    @Schema(description = "收货人姓名", example = "张三")
    private String name;

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    @Column(nullable = false, length = 20)
    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址长度不能超过200个字符")
    @Column(nullable = false, length = 200)
    @Schema(description = "详细地址", example = "北京市朝阳区xxx街道xxx号")
    private String address;

    @Column(name = "is_default", nullable = false)
    @Schema(description = "是否为默认地址", example = "false")
    private Boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "创建时间")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // 默认构造函数
    public UserAddress() {}

    // 构造函数
    public UserAddress(Long userId, String name, String phone, String address) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
        return "UserAddress{" +
                "id=" + id +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", isDefault=" + isDefault +
                ", createdAt=" + createdAt +
                '}';
    }
}