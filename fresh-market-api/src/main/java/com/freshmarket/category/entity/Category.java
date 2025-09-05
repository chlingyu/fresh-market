package com.freshmarket.category.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品分类实体
 */
@Entity
@Table(name = "categories")
@Schema(description = "商品分类")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "分类ID")
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    @Column(nullable = false, length = 50)
    @Schema(description = "分类名称", example = "水果")
    private String name;

    @Column(name = "parent_id")
    @Schema(description = "父分类ID", example = "1")
    private Long parentId;

    @Column(name = "sort_order", nullable = false)
    @Schema(description = "排序顺序", example = "1")
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "创建时间")
    private Instant createdAt;

    // JPA关联关系（可选，用于树形结构操作）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Category> children = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // 默认构造函数
    public Category() {}

    // 构造函数
    public Category(String name) {
        this.name = name;
    }

    public Category(String name, Long parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    public Category(String name, Long parentId, Integer sortOrder) {
        this.name = name;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
    }

    // 业务方法
    public boolean isRootCategory() {
        return parentId == null;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public List<Category> getChildren() {
        return children;
    }

    public void setChildren(List<Category> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", parentId=" + parentId +
                ", sortOrder=" + sortOrder +
                ", createdAt=" + createdAt +
                '}';
    }
}