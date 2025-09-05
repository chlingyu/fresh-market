package com.freshmarket.product.repository;

import com.freshmarket.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 商品数据访问层
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 根据分类查询上架商品
     */
    Page<Product> findByCategoryAndActiveTrue(String category, Pageable pageable);

    /**
     * 查询所有上架商品
     */
    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * 根据名称模糊查询上架商品
     */
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    /**
     * 根据价格区间查询上架商品
     */
    Page<Product> findByPriceBetweenAndActiveTrue(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * 根据分类和价格区间查询上架商品
     */
    Page<Product> findByCategoryAndPriceBetweenAndActiveTrue(
            String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * 查询库存低于指定数量的商品
     */
    List<Product> findByStockLessThanAndActiveTrue(Integer stock);

    /**
     * 根据分类统计上架商品数量
     */
    long countByCategoryAndActiveTrue(String category);

    /**
     * 查询所有商品分类
     */
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.active = true ORDER BY p.category")
    List<String> findAllActiveCategories();

    /**
     * 根据ID查询上架商品
     */
    Optional<Product> findByIdAndActiveTrue(Long id);

    /**
     * 批量更新商品状态
     */
    @Modifying
    @Query("UPDATE Product p SET p.active = :active WHERE p.id IN :ids")
    int updateActiveStatusByIds(@Param("active") Boolean active, @Param("ids") List<Long> ids);

    /**
     * 减少库存
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 增加库存
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :id")
    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 原子性扣减库存（带版本检查）
     * 防止超卖问题，同时检查库存充足和版本一致性
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity, p.version = p.version + 1 " +
           "WHERE p.id = :id AND p.stock >= :quantity AND p.version = :version AND p.active = true")
    int decreaseStockWithVersion(@Param("id") Long id, @Param("quantity") Integer quantity, @Param("version") Long version);

    /**
     * 原子性恢复库存（带版本检查）
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity, p.version = p.version + 1 " +
           "WHERE p.id = :id AND p.version = :version")
    int increaseStockWithVersion(@Param("id") Long id, @Param("quantity") Integer quantity, @Param("version") Long version);
}