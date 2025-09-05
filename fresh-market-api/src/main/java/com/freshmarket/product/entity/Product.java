package com.freshmarket.product.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 商品实体
 */
@Entity
@Table(name = "products")
@Schema(description = "商品信息")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "商品ID")
    private Long id;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过100个字符")
    @Column(nullable = false, length = 100)
    @Schema(description = "商品名称", example = "新鲜苹果")
    private String name;

    @Size(max = 500, message = "商品描述长度不能超过500个字符")
    @Column(length = 500)
    @Schema(description = "商品描述", example = "来自烟台的新鲜红富士苹果，口感脆甜")
    private String description;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "价格格式不正确")
    @Column(nullable = false, precision = 10, scale = 2)
    @Schema(description = "商品价格", example = "12.50")
    private BigDecimal price;

    @NotBlank(message = "商品分类不能为空")
    @Size(max = 50, message = "分类长度不能超过50个字符")
    @Column(nullable = false, length = 50)
    @Schema(description = "商品分类", example = "水果")
    private String category;

    @Min(value = 0, message = "库存数量不能为负数")
    @Column(nullable = false)
    @Schema(description = "库存数量", example = "100")
    private Integer stock = 0;

    @Size(max = 20, message = "单位长度不能超过20个字符")
    @Column(length = 20)
    @Schema(description = "商品单位", example = "斤")
    private String unit = "个";

    @Size(max = 255, message = "图片URL长度不能超过255个字符")
    @Column(length = 255)
    @Schema(description = "商品图片URL", example = "https://example.com/apple.jpg")
    private String imageUrl;

    @Column(nullable = false)
    @Schema(description = "是否上架", example = "true")
    private Boolean active = true;

    @DecimalMin(value = "0.0", message = "重量不能为负数")
    @Digits(integer = 5, fraction = 3, message = "重量格式不正确")
    @Column(precision = 8, scale = 3)
    @Schema(description = "商品重量(kg)", example = "0.500")
    private BigDecimal weight;

    @Column(nullable = false, updatable = false)
    @Schema(description = "创建时间")
    private Instant createdAt;

    @Column(nullable = false)
    @Schema(description = "更新时间")
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    @Schema(description = "乐观锁版本号")
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // 默认构造函数
    public Product() {}

    // 构造函数
    public Product(String name, String description, BigDecimal price, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", stock=" + stock +
                ", active=" + active +
                '}';
    }
}