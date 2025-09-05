package com.freshmarket.common.exception;

/**
 * 分类层次异常
 */
public class CategoryHierarchyException extends BusinessException {
    
    private final Long categoryId;
    private final String categoryName;
    private final Integer currentLevel;
    private final Integer maxLevel;

    public CategoryHierarchyException(Long categoryId, String categoryName, 
                                    Integer currentLevel, Integer maxLevel) {
        super("CATEGORY_HIERARCHY_VIOLATION", 
              String.format("分类 %s (ID: %d) 层级超过限制，当前层级 %d，最大允许 %d 级", 
                          categoryName, categoryId, currentLevel, maxLevel));
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.currentLevel = currentLevel;
        this.maxLevel = maxLevel;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public Integer getMaxLevel() {
        return maxLevel;
    }
}