package com.freshmarket.common.exception;

/**
 * 购物车商品未找到异常
 */
public class CartItemNotFoundException extends BusinessException {
    
    private final Long cartItemId;
    private final Long userId;
    private final Long productId;

    public CartItemNotFoundException(Long cartItemId, Long userId) {
        super("CART_ITEM_NOT_FOUND", 
              String.format("购物车商品 (ID: %d) 不存在或不属于用户 %d", cartItemId, userId));
        this.cartItemId = cartItemId;
        this.userId = userId;
        this.productId = null;
    }

    public CartItemNotFoundException(Long userId, Long productId) {
        super("CART_ITEM_NOT_FOUND", 
              String.format("用户 %d 的购物车中未找到商品 %d", userId, productId));
        this.cartItemId = null;
        this.userId = userId;
        this.productId = productId;
    }

    public Long getCartItemId() {
        return cartItemId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getProductId() {
        return productId;
    }
}