package com.freshmarket.user.service;

import com.freshmarket.user.dto.*;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    UserResponse register(UserRegisterRequest request);
    
    /**
     * 用户登录
     */
    LoginResponse login(UserLoginRequest request);
    
    /**
     * 根据用户ID获取用户信息
     */
    UserResponse getUserById(Long userId);
    
    /**
     * 更新用户信息
     */
    UserResponse updateUser(Long userId, UserUpdateRequest request);
}