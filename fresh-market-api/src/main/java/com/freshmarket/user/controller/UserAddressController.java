package com.freshmarket.user.controller;

import com.freshmarket.common.dto.BaseResponse;
import com.freshmarket.security.SecurityUtils;
import com.freshmarket.user.dto.AddressRequest;
import com.freshmarket.user.dto.AddressResponse;
import com.freshmarket.user.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户地址控制器
 */
@RestController
@RequestMapping("/api/v1/users/addresses")
@Tag(name = "用户地址管理", description = "用户收货地址的增删改查相关API")
@SecurityRequirement(name = "bearerAuth")
public class UserAddressController {

    private final UserAddressService addressService;

    public UserAddressController(UserAddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "创建用户地址",
        description = "为当前用户创建新的收货地址",
        responses = {
            @ApiResponse(responseCode = "201", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "409", description = "地址数量超出限制")
        }
    )
    public BaseResponse<AddressResponse> createAddress(@Valid @RequestBody AddressRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        AddressResponse address = addressService.createAddress(userId, request);
        return BaseResponse.success(address);
    }

    @PutMapping("/{addressId}")
    @Operation(
        summary = "更新用户地址",
        description = "更新指定的用户收货地址",
        responses = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "地址不存在")
        }
    )
    public BaseResponse<AddressResponse> updateAddress(
            @Parameter(description = "地址ID", example = "1")
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        AddressResponse address = addressService.updateAddress(userId, addressId, request);
        return BaseResponse.success(address);
    }

    @PutMapping("/{addressId}/default")
    @Operation(
        summary = "设置默认地址",
        description = "将指定地址设置为默认收货地址",
        responses = {
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "地址不存在")
        }
    )
    public BaseResponse<Void> setDefaultAddress(
            @Parameter(description = "地址ID", example = "1")
            @PathVariable Long addressId) {
        Long userId = SecurityUtils.getCurrentUserId();
        addressService.setDefaultAddress(userId, addressId);
        return BaseResponse.success();
    }

    @DeleteMapping("/{addressId}")
    @Operation(
        summary = "删除用户地址",
        description = "删除指定的用户收货地址",
        responses = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "地址不存在")
        }
    )
    public BaseResponse<Void> deleteAddress(
            @Parameter(description = "地址ID", example = "1")
            @PathVariable Long addressId) {
        Long userId = SecurityUtils.getCurrentUserId();
        addressService.deleteAddress(userId, addressId);
        return BaseResponse.success();
    }

    @GetMapping("/{addressId}")
    @Operation(
        summary = "获取地址详情",
        description = "获取指定地址的详细信息",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "地址不存在")
        }
    )
    public BaseResponse<AddressResponse> getAddress(
            @Parameter(description = "地址ID", example = "1")
            @PathVariable Long addressId) {
        Long userId = SecurityUtils.getCurrentUserId();
        AddressResponse address = addressService.getAddress(userId, addressId);
        return BaseResponse.success(address);
    }

    @GetMapping
    @Operation(
        summary = "获取用户地址列表",
        description = "获取当前用户的所有收货地址列表，默认地址排在前面",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<List<AddressResponse>> getUserAddresses() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<AddressResponse> addresses = addressService.getUserAddresses(userId);
        return BaseResponse.success(addresses);
    }

    @GetMapping("/default")
    @Operation(
        summary = "获取默认地址",
        description = "获取当前用户的默认收货地址",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "用户没有设置默认地址")
        }
    )
    public BaseResponse<AddressResponse> getDefaultAddress() {
        Long userId = SecurityUtils.getCurrentUserId();
        AddressResponse address = addressService.getDefaultAddress(userId);
        return BaseResponse.success(address);
    }

    @GetMapping("/count")
    @Operation(
        summary = "获取地址数量",
        description = "获取当前用户的收货地址数量",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<Long> getUserAddressCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        long count = addressService.getUserAddressCount(userId);
        return BaseResponse.success(count);
    }
}