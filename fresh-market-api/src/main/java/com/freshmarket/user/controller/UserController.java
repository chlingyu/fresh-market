package com.freshmarket.user.controller;

import com.freshmarket.common.dto.BaseResponse;
import com.freshmarket.security.UserPrincipal;
import com.freshmarket.user.dto.*;
import com.freshmarket.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户注册、登录、信息管理相关API")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "用户注册",
        description = "创建新用户账号",
        responses = {
            @ApiResponse(responseCode = "201", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "409", description = "用户名或邮箱已存在")
        }
    )
    public BaseResponse<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        UserResponse user = userService.register(request);
        return BaseResponse.success(user);
    }
    
    @PostMapping("/login")
    @Operation(
        summary = "用户登录",
        description = "用户认证并获取访问令牌",
        responses = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误")
        }
    )
    public BaseResponse<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        LoginResponse response = userService.login(request);
        return BaseResponse.success(response);
    }
    
    @GetMapping("/profile")
    @Operation(
        summary = "获取用户信息",
        description = "获取当前登录用户的详细信息",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<UserResponse> getUserProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponse user = userService.getUserById(userPrincipal.getUserId());
        return BaseResponse.success(user);
    }
    
    @PutMapping("/profile")
    @Operation(
        summary = "更新用户信息",
        description = "更新当前登录用户的信息",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<UserResponse> updateUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse user = userService.updateUser(userPrincipal.getUserId(), request);
        return BaseResponse.success(user);
    }
}