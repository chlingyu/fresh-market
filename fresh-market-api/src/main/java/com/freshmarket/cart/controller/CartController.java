package com.freshmarket.cart.controller;

import com.freshmarket.cart.dto.CartItemRequest;
import com.freshmarket.cart.dto.CartItemResponse;
import com.freshmarket.cart.dto.CartSummaryResponse;
import com.freshmarket.cart.service.CartService;
import com.freshmarket.common.dto.BaseResponse;
import com.freshmarket.security.SecurityUtils;
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
 * 购物车控制器
 */
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "购物车管理", description = "购物车的增删改查相关API")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "添加商品到购物车",
        description = "将指定商品添加到当前用户的购物车中",
        responses = {
            @ApiResponse(responseCode = "201", description = "添加成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "商品不存在"),
            @ApiResponse(responseCode = "409", description = "库存不足")
        }
    )
    public BaseResponse<CartItemResponse> addToCart(@Valid @RequestBody CartItemRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CartItemResponse cartItem = cartService.addToCart(userId, request);
        return BaseResponse.success(cartItem);
    }

    @PutMapping("/items/{productId}")
    @Operation(
        summary = "更新购物车商品数量",
        description = "更新购物车中指定商品的数量",
        responses = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "商品不存在"),
            @ApiResponse(responseCode = "409", description = "库存不足")
        }
    )
    public BaseResponse<CartItemResponse> updateCartItem(
            @Parameter(description = "商品ID", example = "1")
            @PathVariable Long productId,
            
            @Parameter(description = "商品数量", example = "3")
            @RequestParam Integer quantity) {
        Long userId = SecurityUtils.getCurrentUserId();
        CartItemResponse cartItem = cartService.updateCartItem(userId, productId, quantity);
        return BaseResponse.success(cartItem);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(
        summary = "从购物车删除商品",
        description = "从购物车中删除指定商品",
        responses = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
        }
    )
    public BaseResponse<Void> removeFromCart(
            @Parameter(description = "商品ID", example = "1")
            @PathVariable Long productId) {
        Long userId = SecurityUtils.getCurrentUserId();
        cartService.removeFromCart(userId, productId);
        return BaseResponse.success();
    }

    @DeleteMapping("/items")
    @Operation(
        summary = "批量删除购物车商品",
        description = "从购物车中批量删除指定商品",
        responses = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<Void> removeFromCart(
            @Parameter(description = "商品ID列表")
            @RequestParam List<Long> productIds) {
        Long userId = SecurityUtils.getCurrentUserId();
        cartService.removeFromCart(userId, productIds);
        return BaseResponse.success();
    }

    @DeleteMapping("/clear")
    @Operation(
        summary = "清空购物车",
        description = "清空当前用户的购物车",
        responses = {
            @ApiResponse(responseCode = "200", description = "清空成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<Void> clearCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        cartService.clearCart(userId);
        return BaseResponse.success();
    }

    @GetMapping("/summary")
    @Operation(
        summary = "获取购物车汇总",
        description = "获取当前用户购物车的详细汇总信息",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<CartSummaryResponse> getCartSummary() {
        Long userId = SecurityUtils.getCurrentUserId();
        CartSummaryResponse summary = cartService.getCartSummary(userId);
        return BaseResponse.success(summary);
    }

    @GetMapping("/count")
    @Operation(
        summary = "获取购物车商品数量",
        description = "获取当前用户购物车中的商品种类数量",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<Long> getCartItemCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        long count = cartService.getCartItemCount(userId);
        return BaseResponse.success(count);
    }

    @GetMapping("/validate")
    @Operation(
        summary = "验证购物车有效性",
        description = "检查购物车中的商品是否还有效（是否下架、库存是否充足等）",
        responses = {
            @ApiResponse(responseCode = "200", description = "验证完成"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<List<CartItemResponse>> validateCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<CartItemResponse> invalidItems = cartService.validateCart(userId);
        return BaseResponse.success(invalidItems);
    }
}