package com.freshmarket.category.controller;

import com.freshmarket.category.dto.CategoryRequest;
import com.freshmarket.category.dto.CategoryResponse;
import com.freshmarket.category.service.CategoryService;
import com.freshmarket.common.dto.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类控制器
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "商品分类管理", description = "商品分类的增删改查、树形结构管理相关API")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "创建分类",
        description = "创建新的商品分类，需要管理员权限",
        responses = {
            @ApiResponse(responseCode = "201", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "409", description = "分类名称已存在")
        }
    )
    public BaseResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return BaseResponse.success(category);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "更新分类",
        description = "更新指定分类的信息，需要管理员权限",
        responses = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "404", description = "分类不存在"),
            @ApiResponse(responseCode = "409", description = "分类名称已存在")
        }
    )
    public BaseResponse<CategoryResponse> updateCategory(
            @Parameter(description = "分类ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.updateCategory(id, request);
        return BaseResponse.success(category);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "删除分类",
        description = "删除指定分类，需要管理员权限。只能删除没有子分类和商品的分类",
        responses = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "404", description = "分类不存在"),
            @ApiResponse(responseCode = "409", description = "分类下有子分类或商品，无法删除")
        }
    )
    public BaseResponse<Void> deleteCategory(
            @Parameter(description = "分类ID", example = "1")
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return BaseResponse.success();
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取分类详情",
        description = "根据分类ID获取分类的详细信息",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "分类不存在")
        }
    )
    public BaseResponse<CategoryResponse> getCategory(
            @Parameter(description = "分类ID", example = "1")
            @PathVariable Long id) {
        CategoryResponse category = categoryService.getCategory(id);
        return BaseResponse.success(category);
    }

    @GetMapping("/tree")
    @Operation(
        summary = "获取分类树",
        description = "获取完整的分类树形结构，包含所有层级的分类和商品数量统计",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功")
        }
    )
    public BaseResponse<List<CategoryResponse>> getCategoryTree() {
        List<CategoryResponse> categoryTree = categoryService.getCategoryTree();
        return BaseResponse.success(categoryTree);
    }

    @GetMapping
    @Operation(
        summary = "获取分类列表",
        description = "获取所有分类的扁平列表，按排序顺序返回",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功")
        }
    )
    public BaseResponse<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return BaseResponse.success(categories);
    }

    @GetMapping("/children/{parentId}")
    @Operation(
        summary = "获取子分类列表",
        description = "获取指定分类下的直接子分类列表",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "父分类不存在")
        }
    )
    public BaseResponse<List<CategoryResponse>> getChildCategories(
            @Parameter(description = "父分类ID，传null获取根分类", example = "1")
            @PathVariable(required = false) Long parentId) {
        List<CategoryResponse> children = categoryService.getChildCategories(parentId);
        return BaseResponse.success(children);
    }

    @GetMapping("/roots")
    @Operation(
        summary = "获取根分类列表",
        description = "获取所有根级分类列表",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功")
        }
    )
    public BaseResponse<List<CategoryResponse>> getRootCategories() {
        List<CategoryResponse> rootCategories = categoryService.getChildCategories(null);
        return BaseResponse.success(rootCategories);
    }

    @GetMapping("/search")
    @Operation(
        summary = "搜索分类",
        description = "根据分类名称进行模糊搜索",
        responses = {
            @ApiResponse(responseCode = "200", description = "搜索成功")
        }
    )
    public BaseResponse<List<CategoryResponse>> searchCategories(
            @Parameter(description = "分类名称关键词", example = "水果")
            @RequestParam String name) {
        List<CategoryResponse> categories = categoryService.searchCategories(name);
        return BaseResponse.success(categories);
    }

    @GetMapping("/{id}/path")
    @Operation(
        summary = "获取分类路径",
        description = "获取指定分类的完整路径（从根分类到当前分类）",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "分类不存在")
        }
    )
    public BaseResponse<List<CategoryResponse>> getCategoryPath(
            @Parameter(description = "分类ID", example = "1")
            @PathVariable Long id) {
        List<CategoryResponse> path = categoryService.getCategoryPath(id);
        return BaseResponse.success(path);
    }
}