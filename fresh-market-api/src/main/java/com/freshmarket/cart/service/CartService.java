package com.freshmarket.cart.service;

import com.freshmarket.cart.dto.CartItemRequest;
import com.freshmarket.cart.dto.CartItemResponse;
import com.freshmarket.cart.dto.CartSummaryResponse;
import com.freshmarket.cart.entity.CartItem;
import com.freshmarket.cart.repository.CartItemRepository;
import com.freshmarket.common.exception.ResourceNotFoundException;
import com.freshmarket.product.entity.Product;
import com.freshmarket.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务
 */
@Service
@Transactional
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    /**
     * 添加商品到购物车
     */
    public CartItemResponse addToCart(Long userId, CartItemRequest request) {
        logger.debug("Adding product {} to cart for user {}", request.getProductId(), userId);

        // 验证商品是否存在且上架
        Product product = productRepository.findByIdAndActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在或已下架"));

        // 检查库存
        if (product.getStock() < request.getQuantity()) {
            throw new IllegalStateException(String.format("商品库存不足，可用库存：%d，需要：%d", 
                    product.getStock(), request.getQuantity()));
        }

        // 查看购物车中是否已存在该商品
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId());

        CartItem cartItem;
        if (existingItem.isPresent()) {
            // 更新数量
            cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();
            
            // 再次检查库存
            if (product.getStock() < newQuantity) {
                throw new IllegalStateException(String.format("商品库存不足，可用库存：%d，购物车已有：%d，新增：%d", 
                        product.getStock(), cartItem.getQuantity(), request.getQuantity()));
            }
            
            cartItem.setQuantity(newQuantity);
        } else {
            // 创建新的购物车项
            cartItem = new CartItem(userId, request.getProductId(), request.getQuantity());
        }

        CartItem savedItem = cartItemRepository.save(cartItem);
        logger.info("Added product {} to cart for user {}, quantity: {}", 
                request.getProductId(), userId, request.getQuantity());

        return mapToCartItemResponse(savedItem, product);
    }

    /**
     * 更新购物车商品数量
     */
    public CartItemResponse updateCartItem(Long userId, Long productId, Integer quantity) {
        logger.debug("Updating cart item for user {}, product {}, quantity: {}", userId, productId, quantity);

        if (quantity <= 0) {
            throw new IllegalArgumentException("商品数量必须大于0");
        }

        // 查找购物车项
        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("购物车中未找到该商品"));

        // 验证商品库存
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在或已下架"));

        if (product.getStock() < quantity) {
            throw new IllegalStateException(String.format("商品库存不足，可用库存：%d，需要：%d", 
                    product.getStock(), quantity));
        }

        cartItem.setQuantity(quantity);
        CartItem updatedItem = cartItemRepository.save(cartItem);

        logger.info("Updated cart item for user {}, product {}, new quantity: {}", 
                userId, productId, quantity);

        return mapToCartItemResponse(updatedItem, product);
    }

    /**
     * 从购物车删除商品
     */
    public void removeFromCart(Long userId, Long productId) {
        logger.debug("Removing product {} from cart for user {}", productId, userId);

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("购物车中未找到该商品"));

        cartItemRepository.delete(cartItem);
        logger.info("Removed product {} from cart for user {}", productId, userId);
    }

    /**
     * 批量删除购物车商品
     */
    public void removeFromCart(Long userId, List<Long> productIds) {
        logger.debug("Removing {} products from cart for user {}", productIds.size(), userId);

        int deletedCount = cartItemRepository.deleteByUserIdAndProductIds(userId, productIds);
        logger.info("Removed {} products from cart for user {}", deletedCount, userId);
    }

    /**
     * 清空用户购物车
     */
    public void clearCart(Long userId) {
        logger.debug("Clearing cart for user {}", userId);

        cartItemRepository.deleteByUserId(userId);
        logger.info("Cleared cart for user {}", userId);
    }

    /**
     * 获取用户购物车汇总
     */
    @Transactional(readOnly = true)
    public CartSummaryResponse getCartSummary(Long userId) {
        logger.debug("Getting cart summary for user {}", userId);

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        if (cartItems.isEmpty()) {
            return new CartSummaryResponse(List.of());
        }

        // 获取商品信息
        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 转换为响应DTO
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(cartItem -> {
                    Product product = productMap.get(cartItem.getProductId());
                    return mapToCartItemResponse(cartItem, product);
                })
                .collect(Collectors.toList());

        return new CartSummaryResponse(itemResponses);
    }

    /**
     * 获取购物车商品数量
     */
    @Transactional(readOnly = true)
    public long getCartItemCount(Long userId) {
        return cartItemRepository.countByUserId(userId);
    }

    /**
     * 验证购物车商品有效性
     * 返回无效商品列表用于提醒用户
     */
    @Transactional(readOnly = true)
    public List<CartItemResponse> validateCart(Long userId) {
        logger.debug("Validating cart for user {}", userId);

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        if (cartItems.isEmpty()) {
            return List.of();
        }

        // 获取商品信息
        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 只返回无效的购物车项
        return cartItems.stream()
                .map(cartItem -> {
                    Product product = productMap.get(cartItem.getProductId());
                    CartItemResponse response = mapToCartItemResponse(cartItem, product);
                    return response;
                })
                .filter(response -> !Boolean.TRUE.equals(response.getAvailable()))
                .collect(Collectors.toList());
    }

    // 私有辅助方法

    private CartItemResponse mapToCartItemResponse(CartItem cartItem, Product product) {
        CartItemResponse response = new CartItemResponse();
        response.setId(cartItem.getId());
        response.setProductId(cartItem.getProductId());
        response.setQuantity(cartItem.getQuantity());
        response.setCreatedAt(cartItem.getCreatedAt());
        response.setUpdatedAt(cartItem.getUpdatedAt());

        if (product != null && product.getActive()) {
            response.setProductName(product.getName());
            response.setProductPrice(product.getPrice());
            response.setProductImageUrl(product.getImageUrl());
            response.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            
            // 检查库存是否充足
            response.setAvailable(product.getStock() >= cartItem.getQuantity());
        } else {
            response.setProductName("商品已下架");
            response.setProductPrice(BigDecimal.ZERO);
            response.setProductImageUrl(null);
            response.setSubtotal(BigDecimal.ZERO);
            response.setAvailable(false);
        }

        return response;
    }
}