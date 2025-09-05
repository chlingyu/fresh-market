package com.freshmarket.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 商品响应DTO
 */
@Schema(description = "商品信息响应")
public class ProductResponse {

    @Schema(description = "商品ID", example = "1")
    private Long id;

    @Schema(description = "商品名称", example = "新鲜苹果")
    private String name;

    @Schema(description = "商品描述", example = "来自烟台的新鲜红富士苹果，口感脆甜")
    private String description;

    @Schema(description = "商品价格", example = "12.50")
    private BigDecimal price;

    @Schema(description = "商品分类", example = "水果")
    private String category;

    @Schema(description = "库存数量", example = "100")
    private Integer stock;

    @Schema(description = "商品单位", example = "斤")
    private String unit;

    @Schema(description = "商品图片URL", example = "https://example.com/apple.jpg")
    private String imageUrl;

    @Schema(description = "是否上架", example = "true")
    private Boolean active;

    @Schema(description = "商品重量(kg)", example = "0.500")
    private BigDecimal weight;

    @Schema(description = "创建时间")
    private Instant createdAt;

    @Schema(description = "更新时间")
    private Instant updatedAt;

    // 默认构造函数
    public ProductResponse() {}

    // 构造函数
    public ProductResponse(Long id, String name, BigDecimal price, String category) {
        this.id = id;
        this.name = name;
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
}