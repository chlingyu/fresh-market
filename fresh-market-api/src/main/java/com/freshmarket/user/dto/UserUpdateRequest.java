package com.freshmarket.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

/**
 * 用户信息更新请求DTO
 */
@Schema(description = "用户信息更新请求")
public class UserUpdateRequest {
    
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "john@example.com")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    @Schema(description = "手机号码", example = "13812345678")
    private String phone;
    
    // 默认构造函数
    public UserUpdateRequest() {}
    
    // 构造函数
    public UserUpdateRequest(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }
    
    // Getters and Setters
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
}