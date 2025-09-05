package com.freshmarket.product.controller;

import com.freshmarket.common.dto.BaseResponse;
import com.freshmarket.product.dto.ProductRequest;
import com.freshmarket.product.dto.ProductResponse;
import com.freshmarket.product.dto.ProductSearchRequest;
import com.freshmarket.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "商品管理", description = "商品的增删改查、搜索、库存管理相关API")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "创建商品",
        description = "创建新的商品信息，需要管理员权限",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "201", description = "商品创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "403", description = "权限不足")
        }
    )
    public BaseResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return BaseResponse.success(product);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取商品详情",
        description = "根据商品ID获取商品的详细信息",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
        }
    )
    public BaseResponse<ProductResponse> getProduct(
            @Parameter(description = "商品ID", example = "1") 
            @PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return BaseResponse.success(product);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新商品信息",
        description = "更新指定商品的信息，需要管理员权限",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
        }
    )
    public BaseResponse<ProductResponse> updateProduct(
            @Parameter(description = "商品ID", example = "1") 
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.updateProduct(id, request);
        return BaseResponse.success(product);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除商品",
        description = "软删除指定商品（设置为不可用状态），需要管理员权限",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
        }
    )
    public BaseResponse<Void> deleteProduct(
            @Parameter(description = "商品ID", example = "1") 
            @PathVariable Long id) {
        productService.deleteProduct(id);
        return BaseResponse.success();
    }

    @GetMapping
    @Operation(
        summary = "搜索商品",
        description = "根据条件搜索商品，支持分页、排序和多种筛选条件",
        responses = {
            @ApiResponse(responseCode = "200", description = "搜索成功")
        }
    )
    public BaseResponse<Page<ProductResponse>> searchProducts(
            @Parameter(description = "商品名称关键词") 
            @RequestParam(required = false) String name,
            
            @Parameter(description = "商品分类") 
            @RequestParam(required = false) String category,
            
            @Parameter(description = "最低价格") 
            @RequestParam(required = false) String minPrice,
            
            @Parameter(description = "最高价格") 
            @RequestParam(required = false) String maxPrice,
            
            @Parameter(description = "页码(从0开始)") 
            @RequestParam(defaultValue = "0") Integer page,
            
            @Parameter(description = "每页大小") 
            @RequestParam(defaultValue = "20") Integer size,
            
            @Parameter(description = "排序字段") 
            @RequestParam(defaultValue = "id") String sortBy,
            
            @Parameter(description = "排序方向") 
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        ProductSearchRequest searchRequest = new ProductSearchRequest();
        searchRequest.setName(name);
        searchRequest.setCategory(category);
        if (minPrice != null) {
            try {
                searchRequest.setMinPrice(new java.math.BigDecimal(minPrice));
            } catch (NumberFormatException e) {
                // 忽略无效的价格格式
            }
        }
        if (maxPrice != null) {
            try {
                searchRequest.setMaxPrice(new java.math.BigDecimal(maxPrice));
            } catch (NumberFormatException e) {
                // 忽略无效的价格格式
            }
        }
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortDir(sortDir);
        
        Page<ProductResponse> products = productService.searchProducts(searchRequest);
        return BaseResponse.success(products);
    }

    @GetMapping("/categories")
    @Operation(
        summary = "获取所有商品分类",
        description = "获取系统中所有可用的商品分类列表",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功")
        }
    )
    public BaseResponse<List<String>> getAllCategories() {
        List<String> categories = productService.getAllCategories();
        return BaseResponse.success(categories);
    }

    @PatchMapping("/{id}/stock/decrease")
    @Operation(
        summary = "减少库存",
        description = "减少指定商品的库存数量，需要管理员权限",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "库存减少成功"),
            @ApiResponse(responseCode = "400", description = "库存不足或参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
        }
    )
    public BaseResponse<Void> decreaseStock(
            @Parameter(description = "商品ID", example = "1") 
            @PathVariable Long id,
            
            @Parameter(description = "减少数量", example = "5") 
            @RequestParam Integer quantity) {
        productService.decreaseStock(id, quantity);
        return BaseResponse.success();
    }

    @PatchMapping("/{id}/stock/increase")
    @Operation(
        summary = "增加库存",
        description = "增加指定商品的库存数量，需要管理员权限",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "库存增加成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
        }
    )
    public BaseResponse<Void> increaseStock(
            @Parameter(description = "商品ID", example = "1") 
            @PathVariable Long id,
            
            @Parameter(description = "增加数量", example = "10") 
            @RequestParam Integer quantity) {
        productService.increaseStock(id, quantity);
        return BaseResponse.success();
    }

    @GetMapping("/low-stock")
    @Operation(
        summary = "获取低库存商品",
        description = "获取库存低于指定阈值的商品列表，需要管理员权限",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "403", description = "权限不足")
        }
    )
    public BaseResponse<List<ProductResponse>> getLowStockProducts(
            @Parameter(description = "库存阈值", example = "10") 
            @RequestParam(defaultValue = "10") Integer threshold) {
        List<ProductResponse> products = productService.getLowStockProducts(threshold);
        return BaseResponse.success(products);
    }

    @PatchMapping("/batch/status")
    @Operation(
        summary = "批量更新商品状态",
        description = "批量更新多个商品的上架/下架状态，需要管理员权限",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "403", description = "权限不足")
        }
    )
    public BaseResponse<Void> updateProductsStatus(
            @Parameter(description = "商品ID列表") 
            @RequestParam List<Long> ids,
            
            @Parameter(description = "状态(true=上架, false=下架)") 
            @RequestParam Boolean active) {
        productService.updateProductsStatus(ids, active);
        return BaseResponse.success();
    }
}