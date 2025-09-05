package com.freshmarket.category.service;

import com.freshmarket.category.dto.CategoryRequest;
import com.freshmarket.category.dto.CategoryResponse;
import com.freshmarket.category.entity.Category;
import com.freshmarket.category.repository.CategoryRepository;
import com.freshmarket.common.exception.DuplicateResourceException;
import com.freshmarket.common.exception.ResourceNotFoundException;
import com.freshmarket.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分类服务
 */
@Service
@Transactional
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    private static final int MAX_CATEGORY_LEVELS = 3; // 最多支持3级分类

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * 创建分类
     */
    @Caching(evict = {
        @CacheEvict(value = "categories", key = "'tree'"),
        @CacheEvict(value = "categories", key = "'all'"),
        @CacheEvict(value = "categories", key = "'children:' + (#request.parentId != null ? #request.parentId : 'root')")
    })
    public CategoryResponse createCategory(CategoryRequest request) {
        logger.debug("Creating category: {}", request.getName());

        // 验证父分类存在性和层级限制
        if (request.getParentId() != null) {
            Category parentCategory = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("父分类不存在"));

            // 检查层级限制
            int parentLevel = calculateCategoryLevel(parentCategory);
            if (parentLevel >= MAX_CATEGORY_LEVELS - 1) {
                throw new IllegalStateException(String.format("分类层级不能超过%d级", MAX_CATEGORY_LEVELS));
            }
        }

        // 检查同级分类名称重复
        if (categoryRepository.findByNameAndParentId(request.getName(), request.getParentId()).isPresent()) {
            throw new DuplicateResourceException("同级分类中已存在相同名称的分类");
        }

        Category category = new Category(request.getName(), request.getParentId(), request.getSortOrder());
        Category savedCategory = categoryRepository.save(category);

        logger.info("Category created successfully with ID: {}", savedCategory.getId());
        return mapToResponse(savedCategory);
    }

    /**
     * 更新分类
     */
    @Caching(evict = {
        @CacheEvict(value = "categories", allEntries = true)
    })
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        logger.debug("Updating category: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));

        // 如果要修改父分类
        if (!java.util.Objects.equals(category.getParentId(), request.getParentId())) {
            // 验证不能将分类移动到自己的子分类下
            if (request.getParentId() != null) {
                List<Long> childIds = categoryRepository.findAllChildCategoryIds(categoryId);
                if (childIds.contains(request.getParentId())) {
                    throw new IllegalStateException("不能将分类移动到自己的子分类下");
                }

                // 检查目标父分类存在性和层级限制
                Category newParentCategory = categoryRepository.findById(request.getParentId())
                        .orElseThrow(() -> new ResourceNotFoundException("目标父分类不存在"));

                int newParentLevel = calculateCategoryLevel(newParentCategory);
                int currentSubtreeDepth = calculateSubtreeDepth(categoryId);
                if (newParentLevel + currentSubtreeDepth >= MAX_CATEGORY_LEVELS) {
                    throw new IllegalStateException(String.format("移动后的分类层级将超过%d级限制", MAX_CATEGORY_LEVELS));
                }
            }
        }

        // 检查同级分类名称重复
        if (categoryRepository.findByNameAndParentIdAndIdNot(request.getName(), request.getParentId(), categoryId).isPresent()) {
            throw new DuplicateResourceException("同级分类中已存在相同名称的分类");
        }

        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setSortOrder(request.getSortOrder());

        Category updatedCategory = categoryRepository.save(category);
        logger.info("Category updated successfully: {}", categoryId);

        return mapToResponse(updatedCategory);
    }

    /**
     * 删除分类
     */
    @Caching(evict = {
        @CacheEvict(value = "categories", allEntries = true)
    })
    public void deleteCategory(Long categoryId) {
        logger.debug("Deleting category: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));

        // 检查是否有子分类
        if (categoryRepository.existsByParentId(categoryId)) {
            throw new IllegalStateException("该分类下还有子分类，无法删除");
        }

        // 检查是否有商品使用该分类
        long productCount = productRepository.countByCategoryAndActiveTrue(category.getName());
        if (productCount > 0) {
            throw new IllegalStateException(String.format("该分类下还有%d个商品，无法删除", productCount));
        }

        categoryRepository.delete(category);
        logger.info("Category deleted successfully: {}", categoryId);
    }

    /**
     * 获取分类详情
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long categoryId) {
        logger.debug("Getting category: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));

        return mapToResponse(category);
    }

    /**
     * 获取分类树结构
     */
    @Cacheable(value = "categories", key = "'tree'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        logger.debug("Getting category tree");

        List<Category> rootCategories = categoryRepository.findRootCategoriesWithParent();
        return buildCategoryTree(rootCategories);
    }

    /**
     * 获取指定分类的子分类列表
     */
    @Cacheable(value = "categories", key = "'children:' + (#parentId != null ? #parentId : 'root')")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildCategories(Long parentId) {
        logger.debug("Getting child categories for parent: {}", parentId);

        // 验证父分类存在
        if (parentId != null && !categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("父分类不存在");
        }

        List<Category> childCategories = parentId == null ? 
            categoryRepository.findRootCategoriesWithParent() :
            categoryRepository.findByParentIdWithParentFetch(parentId);
        return childCategories.stream()
                .map(this::mapToResponseOptimized)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有分类列表（扁平结构）
     */
    @Cacheable(value = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        logger.debug("Getting all categories");

        List<Category> categories = categoryRepository.findAllWithParentFetch();
        return categories.stream()
                .map(this::mapToResponseOptimized)
                .collect(Collectors.toList());
    }

    /**
     * 搜索分类
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> searchCategories(String name) {
        logger.debug("Searching categories with name: {}", name);

        List<Category> categories = categoryRepository.findByNameContainingIgnoreCaseOrderBySortOrderAscNameAsc(name);
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取分类路径
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryPath(Long categoryId) {
        logger.debug("Getting category path for: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));

        List<Category> path = new ArrayList<>();
        Category current = category;
        
        while (current != null) {
            path.add(0, current); // 添加到列表开头
            if (current.getParentId() != null) {
                current = categoryRepository.findById(current.getParentId()).orElse(null);
            } else {
                break;
            }
        }

        return path.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 私有辅助方法

    private List<CategoryResponse> buildCategoryTree(List<Category> categories) {
        // 获取所有分类的商品数量
        Map<String, Long> productCountMap = getProductCountMap();

        return categories.stream()
                .map(category -> {
                    CategoryResponse response = mapToResponse(category);
                    response.setProductCount(productCountMap.getOrDefault(category.getName(), 0L));
                    
                    // 递归构建子分类树
                    List<Category> children = categoryRepository.findByParentIdOrderBySortOrderAscNameAsc(category.getId());
                    if (!children.isEmpty()) {
                        response.setChildren(buildCategoryTree(children));
                        response.setHasChildren(true);
                    } else {
                        response.setHasChildren(false);
                    }
                    
                    return response;
                })
                .collect(Collectors.toList());
    }

    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setParentId(category.getParentId());
        response.setSortOrder(category.getSortOrder());
        response.setCreatedAt(category.getCreatedAt());

        // 设置父分类名称
        if (category.getParentId() != null) {
            categoryRepository.findById(category.getParentId())
                    .ifPresent(parent -> response.setParentName(parent.getName()));
        }

        // 设置分类层级
        response.setLevel(calculateCategoryLevel(category));

        // 设置是否有子分类
        response.setHasChildren(categoryRepository.existsByParentId(category.getId()));

        return response;
    }

    /**
     * 优化版本的映射方法（避免N+1查询）
     * 适用于已经预加载了parent关系的Category对象
     */
    private CategoryResponse mapToResponseOptimized(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setParentId(category.getParentId());
        response.setSortOrder(category.getSortOrder());
        response.setCreatedAt(category.getCreatedAt());

        // 从预加载的关系中设置父分类名称（避免额外查询）
        if (category.getParent() != null) {
            response.setParentName(category.getParent().getName());
        }

        // 使用优化的层级计算
        response.setLevel(calculateCategoryLevelOptimized(category));

        // 设置是否有子分类
        response.setHasChildren(categoryRepository.existsByParentId(category.getId()));

        return response;
    }

    private int calculateCategoryLevel(Category category) {
        int level = 1;
        Category current = category;
        
        while (current.getParentId() != null) {
            level++;
            current = categoryRepository.findById(current.getParentId()).orElse(null);
            if (current == null) break;
        }
        
        return level;
    }

    /**
     * 优化版本的层级计算（适用于预加载了parent关系的Category）
     */
    private int calculateCategoryLevelOptimized(Category category) {
        int level = 1;
        Category current = category;
        
        while (current.getParent() != null) {
            level++;
            current = current.getParent();
            // 防止循环引用导致的无限循环
            if (level > MAX_CATEGORY_LEVELS) {
                logger.warn("Category level exceeded maximum: {}", level);
                break;
            }
        }
        
        return level;
    }

    private int calculateSubtreeDepth(Long categoryId) {
        List<Long> childIds = categoryRepository.findAllChildCategoryIds(categoryId);
        if (childIds.isEmpty()) {
            return 1;
        }
        
        // 简化版本：假设最大深度为当前层级数
        // 在实际生产环境中，可能需要更精确的递归计算
        return childIds.size() > 0 ? 2 : 1;
    }

    private Map<String, Long> getProductCountMap() {
        // 获取每个分类的商品数量
        Map<String, Long> countMap = new HashMap<>();
        List<String> allCategories = categoryRepository.findAll().stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        
        for (String categoryName : allCategories) {
            long count = productRepository.countByCategoryAndActiveTrue(categoryName);
            countMap.put(categoryName, count);
        }
        
        return countMap;
    }
}