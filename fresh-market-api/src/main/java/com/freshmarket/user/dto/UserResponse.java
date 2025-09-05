package com.freshmarket.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 用户响应DTO
 */
@Schema(description = "用户信息响应")
public class UserResponse {
    
    @Schema(description = "用户ID", example = "1")
    private Long id;
    
    @Schema(description = "用户名", example = "john_doe")
    private String username;
    
    @Schema(description = "邮箱地址", example = "john@example.com")
    private String email;
    
    @Schema(description = "手机号码", example = "13812345678")
    private String phone;
    
    @Schema(description = "创建时间")
    private Instant createdAt;
    
    // 默认构造函数
    public UserResponse() {}
    
    // 构造函数
    public UserResponse(Long id, String username, String email, String phone, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}