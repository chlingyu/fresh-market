package com.freshmarket.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * 商品分类响应DTO
 */
@Schema(description = "商品分类信息")
public class CategoryResponse {

    @Schema(description = "分类ID", example = "1")
    private Long id;

    @Schema(description = "分类名称", example = "水果")
    private String name;

    @Schema(description = "父分类ID", example = "1")
    private Long parentId;

    @Schema(description = "父分类名称", example = "生鲜")
    private String parentName;

    @Schema(description = "排序顺序", example = "1")
    private Integer sortOrder;

    @Schema(description = "子分类列表")
    private List<CategoryResponse> children;

    @Schema(description = "是否有子分类", example = "true")
    private Boolean hasChildren;

    @Schema(description = "分类层级", example = "1")
    private Integer level;

    @Schema(description = "商品数量", example = "25")
    private Long productCount;

    @Schema(description = "创建时间")
    private Instant createdAt;

    // 默认构造函数
    public CategoryResponse() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<CategoryResponse> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryResponse> children) {
        this.children = children;
    }

    public Boolean getHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(Boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Long getProductCount() {
        return productCount;
    }

    public void setProductCount(Long productCount) {
        this.productCount = productCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "CategoryResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", parentId=" + parentId +
                ", sortOrder=" + sortOrder +
                ", hasChildren=" + hasChildren +
                ", level=" + level +
                ", productCount=" + productCount +
                '}';
    }
}