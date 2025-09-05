package com.freshmarket.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 商品分类请求DTO
 */
@Schema(description = "商品分类请求")
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    @Schema(description = "分类名称", example = "水果", required = true)
    private String name;

    @Schema(description = "父分类ID", example = "1")
    private Long parentId;

    @Schema(description = "排序顺序", example = "1")
    private Integer sortOrder = 0;

    // 默认构造函数
    public CategoryRequest() {}

    // 构造函数
    public CategoryRequest(String name) {
        this.name = name;
    }

    public CategoryRequest(String name, Long parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    public CategoryRequest(String name, Long parentId, Integer sortOrder) {
        this.name = name;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
    }

    // Getters and Setters
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public String toString() {
        return "CategoryRequest{" +
                "name='" + name + '\'' +
                ", parentId=" + parentId +
                ", sortOrder=" + sortOrder +
                '}';
    }
}