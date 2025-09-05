package com.freshmarket.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 商品搜索请求DTO
 */
@Schema(description = "商品搜索请求")
public class ProductSearchRequest {

    @Size(max = 100, message = "商品名称长度不能超过100个字符")
    @Schema(description = "商品名称关键词", example = "苹果")
    private String name;

    @Size(max = 50, message = "分类长度不能超过50个字符")
    @Schema(description = "商品分类", example = "水果")
    private String category;

    @DecimalMin(value = "0.00", message = "最低价格不能为负数")
    @Digits(integer = 8, fraction = 2, message = "价格格式不正确")
    @Schema(description = "最低价格", example = "0.00")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.01", message = "最高价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "价格格式不正确")
    @Schema(description = "最高价格", example = "100.00")
    private BigDecimal maxPrice;

    @Min(value = 0, message = "页码不能为负数")
    @Schema(description = "页码(从0开始)", example = "0")
    private Integer page = 0;

    @Min(value = 1, message = "每页大小必须大于0")
    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "排序字段", example = "price", 
           allowableValues = {"id", "name", "price", "createdAt", "updatedAt"})
    private String sortBy = "id";

    @Schema(description = "排序方向", example = "asc", 
           allowableValues = {"asc", "desc"})
    private String sortDir = "asc";

    // 默认构造函数
    public ProductSearchRequest() {}

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }
}