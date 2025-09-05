package com.freshmarket.product.service;

import com.freshmarket.common.exception.ResourceNotFoundException;
import com.freshmarket.product.dto.ProductRequest;
import com.freshmarket.product.dto.ProductResponse;
import com.freshmarket.product.dto.ProductSearchRequest;
import com.freshmarket.product.entity.Product;
import com.freshmarket.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务
 */
@Service
@Transactional
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 创建商品
     */
    public ProductResponse createProduct(ProductRequest request) {
        logger.debug("Creating product: {}", request.getName());
        
        Product product = new Product();
        mapRequestToEntity(request, product);
        
        Product savedProduct = productRepository.save(product);
        logger.info("Product created successfully with ID: {}", savedProduct.getId());
        
        return mapEntityToResponse(savedProduct);
    }

    /**
     * 更新商品
     */
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        logger.debug("Updating product with ID: {}", productId);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        mapRequestToEntity(request, product);
        
        Product updatedProduct = productRepository.save(product);
        logger.info("Product updated successfully with ID: {}", updatedProduct.getId());
        
        return mapEntityToResponse(updatedProduct);
    }

    /**
     * 根据ID获取商品详情
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        logger.debug("Getting product by ID: {}", productId);
        
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        return mapEntityToResponse(product);
    }

    /**
     * 搜索商品
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductSearchRequest searchRequest) {
        logger.debug("Searching products with criteria: {}", searchRequest);
        
        Pageable pageable = createPageable(searchRequest);
        Page<Product> products = findProductsBySearchCriteria(searchRequest, pageable);
        
        return products.map(this::mapEntityToResponse);
    }

    /**
     * 获取所有商品分类
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        logger.debug("Getting all product categories");
        return productRepository.findAllActiveCategories();
    }

    /**
     * 删除商品(软删除)
     */
    public void deleteProduct(Long productId) {
        logger.debug("Deleting product with ID: {}", productId);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        product.setActive(false);
        productRepository.save(product);
        
        logger.info("Product deleted successfully with ID: {}", productId);
    }

    /**
     * 批量更新商品状态
     */
    public void updateProductsStatus(List<Long> productIds, Boolean active) {
        logger.debug("Updating status for {} products to: {}", productIds.size(), active);
        
        int updatedCount = productRepository.updateActiveStatusByIds(active, productIds);
        logger.info("Updated status for {} products", updatedCount);
    }

    /**
     * 减少库存
     */
    public void decreaseStock(Long productId, Integer quantity) {
        logger.debug("Decreasing stock for product {}: -{}", productId, quantity);
        
        int updatedCount = productRepository.decreaseStock(productId, quantity);
        if (updatedCount == 0) {
            // 检查产品是否存在或库存不足
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
            
            if (product.getStock() < quantity) {
                throw new IllegalStateException("Insufficient stock. Available: " + product.getStock() + ", Required: " + quantity);
            }
        }
        
        logger.info("Stock decreased for product {} by {}", productId, quantity);
    }

    /**
     * 增加库存
     */
    public void increaseStock(Long productId, Integer quantity) {
        logger.debug("Increasing stock for product {}: +{}", productId, quantity);
        
        // 先验证产品存在
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        productRepository.increaseStock(productId, quantity);
        logger.info("Stock increased for product {} by {}", productId, quantity);
    }

    /**
     * 获取低库存商品
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(Integer threshold) {
        logger.debug("Getting products with stock below: {}", threshold);
        
        List<Product> products = productRepository.findByStockLessThanAndActiveTrue(threshold);
        return products.stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    // 私有辅助方法
    
    private void mapRequestToEntity(ProductRequest request, Product product) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStock(request.getStock() != null ? request.getStock() : 0);
        product.setUnit(request.getUnit() != null ? request.getUnit() : "个");
        product.setImageUrl(request.getImageUrl());
        product.setActive(request.getActive() != null ? request.getActive() : true);
        product.setWeight(request.getWeight());
    }

    private ProductResponse mapEntityToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setCategory(product.getCategory());
        response.setStock(product.getStock());
        response.setUnit(product.getUnit());
        response.setImageUrl(product.getImageUrl());
        response.setActive(product.getActive());
        response.setWeight(product.getWeight());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }

    private Pageable createPageable(ProductSearchRequest searchRequest) {
        Sort.Direction direction = "desc".equalsIgnoreCase(searchRequest.getSortDir()) 
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, searchRequest.getSortBy());
        return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
    }

    private Page<Product> findProductsBySearchCriteria(ProductSearchRequest searchRequest, Pageable pageable) {
        String name = searchRequest.getName();
        String category = searchRequest.getCategory();
        BigDecimal minPrice = searchRequest.getMinPrice();
        BigDecimal maxPrice = searchRequest.getMaxPrice();

        // 根据不同的搜索条件组合调用不同的Repository方法
        if (StringUtils.hasText(name)) {
            return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name, pageable);
        } else if (StringUtils.hasText(category) && minPrice != null && maxPrice != null) {
            return productRepository.findByCategoryAndPriceBetweenAndActiveTrue(category, minPrice, maxPrice, pageable);
        } else if (StringUtils.hasText(category)) {
            return productRepository.findByCategoryAndActiveTrue(category, pageable);
        } else if (minPrice != null && maxPrice != null) {
            return productRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice, pageable);
        } else {
            return productRepository.findByActiveTrue(pageable);
        }
    }
}