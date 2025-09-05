package com.freshmarket.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录响应DTO
 */
@Schema(description = "登录响应")
public class LoginResponse {
    
    @Schema(description = "用户信息")
    private UserResponse user;
    
    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIs...")
    private String accessToken;
    
    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";
    
    @Schema(description = "令牌有效期(秒)", example = "86400")
    private Integer expiresIn;
    
    // 默认构造函数
    public LoginResponse() {}
    
    // 构造函数
    public LoginResponse(UserResponse user, String accessToken, Integer expiresIn) {
        this.user = user;
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }
    
    // Getters and Setters
    public UserResponse getUser() {
        return user;
    }
    
    public void setUser(UserResponse user) {
        this.user = user;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public Integer getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }
}