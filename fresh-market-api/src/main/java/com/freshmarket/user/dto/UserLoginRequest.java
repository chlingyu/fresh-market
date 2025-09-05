package com.freshmarket.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO
 */
@Schema(description = "用户登录请求")
public class UserLoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名或邮箱", example = "john_doe")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "password123")
    private String password;
    
    // 默认构造函数
    public UserLoginRequest() {}
    
    // 构造函数
    public UserLoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}