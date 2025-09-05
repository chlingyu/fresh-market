package com.freshmarket.category.repository;

import com.freshmarket.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 商品分类数据访问层
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 根据父分类ID查询子分类列表（按排序顺序）
     */
    List<Category> findByParentIdOrderBySortOrderAscNameAsc(Long parentId);

    /**
     * 查询所有根分类（按排序顺序）
     */
    List<Category> findByParentIdIsNullOrderBySortOrderAscNameAsc();

    /**
     * 查询所有分类（按排序顺序）
     */
    List<Category> findAllByOrderBySortOrderAscNameAsc();

    /**
     * 根据名称查询分类
     */
    Optional<Category> findByName(String name);

    /**
     * 根据名称和父分类ID查询分类（检查同级分类名称重复）
     */
    Optional<Category> findByNameAndParentId(String name, Long parentId);

    /**
     * 根据名称和父分类ID查询分类，排除指定ID（更新时检查重复）
     */
    Optional<Category> findByNameAndParentIdAndIdNot(String name, Long parentId, Long excludeId);

    /**
     * 查询指定分类的所有子分类ID（递归查询）
     */
    @Query(value = """
        WITH RECURSIVE category_tree AS (
            SELECT id, name, parent_id, 0 as level
            FROM categories
            WHERE id = :categoryId
            
            UNION ALL
            
            SELECT c.id, c.name, c.parent_id, ct.level + 1
            FROM categories c
            INNER JOIN category_tree ct ON c.parent_id = ct.id
        )
        SELECT id FROM category_tree WHERE level > 0
        """, nativeQuery = true)
    List<Long> findAllChildCategoryIds(@Param("categoryId") Long categoryId);

    /**
     * 查询指定分类的所有父分类路径
     */
    @Query(value = """
        WITH RECURSIVE category_path AS (
            SELECT id, name, parent_id, 0 as level
            FROM categories
            WHERE id = :categoryId
            
            UNION ALL
            
            SELECT c.id, c.name, c.parent_id, cp.level + 1
            FROM categories c
            INNER JOIN category_path cp ON c.id = cp.parent_id
        )
        SELECT id FROM category_path WHERE level > 0 ORDER BY level DESC
        """, nativeQuery = true)
    List<Long> findAllParentCategoryIds(@Param("categoryId") Long categoryId);

    /**
     * 统计指定父分类下的子分类数量
     */
    long countByParentId(Long parentId);

    /**
     * 统计根分类数量
     */
    long countByParentIdIsNull();

    /**
     * 检查分类是否存在子分类
     */
    boolean existsByParentId(Long parentId);

    /**
     * 查询分类树结构（包含子分类信息）
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parentId IS NULL ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findCategoryTreeWithChildren();

    /**
     * 根据名称模糊搜索分类
     */
    List<Category> findByNameContainingIgnoreCaseOrderBySortOrderAscNameAsc(String name);
}