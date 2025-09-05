package com.freshmarket.cart.repository;

import com.freshmarket.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 购物车数据访问层
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * 根据用户ID查询购物车商品列表
     */
    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 根据用户ID和商品ID查询购物车商品
     */
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * 根据用户ID删除购物车商品
     */
    void deleteByUserId(Long userId);

    /**
     * 根据用户ID和商品ID删除购物车商品
     */
    void deleteByUserIdAndProductId(Long userId, Long productId);

    /**
     * 根据用户ID统计购物车商品数量
     */
    long countByUserId(Long userId);

    /**
     * 根据用户ID和商品ID列表删除购物车商品
     */
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId AND c.productId IN :productIds")
    int deleteByUserIdAndProductIds(@Param("userId") Long userId, @Param("productIds") List<Long> productIds);

    /**
     * 根据商品ID删除所有用户的购物车中该商品
     * 用于商品下架时清理购物车
     */
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.productId = :productId")
    int deleteByProductId(@Param("productId") Long productId);

    /**
     * 批量更新购物车商品数量
     */
    @Modifying
    @Query("UPDATE CartItem c SET c.quantity = :quantity, c.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE c.userId = :userId AND c.productId = :productId")
    int updateQuantity(@Param("userId") Long userId, @Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 查询用户购物车中指定商品ID列表的商品
     */
    @Query("SELECT c FROM CartItem c WHERE c.userId = :userId AND c.productId IN :productIds")
    List<CartItem> findByUserIdAndProductIds(@Param("userId") Long userId, @Param("productIds") List<Long> productIds);
}