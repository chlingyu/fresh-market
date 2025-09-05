package com.freshmarket.product.service;

import com.freshmarket.common.exception.ResourceNotFoundException;
import com.freshmarket.product.dto.ProductRequest;
import com.freshmarket.product.dto.ProductResponse;
import com.freshmarket.product.dto.ProductSearchRequest;
import com.freshmarket.product.entity.Product;
import com.freshmarket.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品Service测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("商品Service测试")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductRequest testProductRequest;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("测试苹果");
        testProduct.setDescription("测试描述");
        testProduct.setPrice(new BigDecimal("12.50"));
        testProduct.setCategory("水果");
        testProduct.setStock(100);
        testProduct.setUnit("斤");
        testProduct.setActive(true);
        testProduct.setWeight(new BigDecimal("0.500"));
        testProduct.setCreatedAt(Instant.now());
        testProduct.setUpdatedAt(Instant.now());

        testProductRequest = new ProductRequest();
        testProductRequest.setName("测试苹果");
        testProductRequest.setDescription("测试描述");
        testProductRequest.setPrice(new BigDecimal("12.50"));
        testProductRequest.setCategory("水果");
        testProductRequest.setStock(100);
        testProductRequest.setUnit("斤");
        testProductRequest.setActive(true);
        testProductRequest.setWeight(new BigDecimal("0.500"));
    }

    @Test
    @DisplayName("应该能成功创建商品")
    void shouldCreateProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductResponse result = productService.createProduct(testProductRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("测试苹果");
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("12.50"));
        assertThat(result.getCategory()).isEqualTo("水果");
        assertThat(result.getStock()).isEqualTo(100);

        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("创建商品时应该设置默认值")
    void shouldSetDefaultValuesWhenCreatingProduct() {
        ProductRequest requestWithNulls = new ProductRequest();
        requestWithNulls.setName("测试商品");
        requestWithNulls.setPrice(new BigDecimal("10.00"));
        requestWithNulls.setCategory("测试");
        // stock, unit, active 为 null

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("测试商品");
        savedProduct.setPrice(new BigDecimal("10.00"));
        savedProduct.setCategory("测试");
        savedProduct.setStock(0);  // 默认值
        savedProduct.setUnit("个"); // 默认值
        savedProduct.setActive(true); // 默认值

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse result = productService.createProduct(requestWithNulls);

        assertThat(result.getStock()).isEqualTo(0);
        assertThat(result.getUnit()).isEqualTo("个");
        assertThat(result.getActive()).isTrue();
    }

    @Test
    @DisplayName("应该能成功更新商品")
    void shouldUpdateProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        testProductRequest.setName("更新后的苹果");
        testProductRequest.setPrice(new BigDecimal("15.00"));

        ProductResponse result = productService.updateProduct(1L, testProductRequest);

        assertThat(result).isNotNull();
        verify(productRepository).findById(1L);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("更新不存在的商品应该抛出异常")
    void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(1L, testProductRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found with id: 1");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("应该能根据ID获取上架商品")
    void shouldGetProductById() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testProduct));

        ProductResponse result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("测试苹果");

        verify(productRepository).findByIdAndActiveTrue(1L);
    }

    @Test
    @DisplayName("获取不存在或已下架的商品应该抛出异常")
    void shouldThrowExceptionWhenGettingNonExistentOrInactiveProduct() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found with id: 1");
    }

    @Test
    @DisplayName("应该能搜索商品")
    void shouldSearchProducts() {
        ProductSearchRequest searchRequest = new ProductSearchRequest();
        searchRequest.setName("苹果");
        searchRequest.setPage(0);
        searchRequest.setSize(20);
        searchRequest.setSortBy("id");
        searchRequest.setSortDir("asc");

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"));
        Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageable, 1);

        when(productRepository.findByNameContainingIgnoreCaseAndActiveTrue("苹果", pageable))
                .thenReturn(productPage);

        Page<ProductResponse> result = productService.searchProducts(searchRequest);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("测试苹果");

        verify(productRepository).findByNameContainingIgnoreCaseAndActiveTrue("苹果", pageable);
    }

    @Test
    @DisplayName("搜索时应该支持分类和价格区间过滤")
    void shouldSearchProductsByCategoryAndPriceRange() {
        ProductSearchRequest searchRequest = new ProductSearchRequest();
        searchRequest.setCategory("水果");
        searchRequest.setMinPrice(new BigDecimal("10.00"));
        searchRequest.setMaxPrice(new BigDecimal("15.00"));
        searchRequest.setPage(0);
        searchRequest.setSize(20);
        searchRequest.setSortBy("price");
        searchRequest.setSortDir("desc");

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "price"));
        Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageable, 1);

        when(productRepository.findByCategoryAndPriceBetweenAndActiveTrue(
                "水果", new BigDecimal("10.00"), new BigDecimal("15.00"), pageable))
                .thenReturn(productPage);

        Page<ProductResponse> result = productService.searchProducts(searchRequest);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(productRepository).findByCategoryAndPriceBetweenAndActiveTrue(
                "水果", new BigDecimal("10.00"), new BigDecimal("15.00"), pageable);
    }

    @Test
    @DisplayName("应该能获取所有商品分类")
    void shouldGetAllCategories() {
        List<String> categories = List.of("水果", "蔬菜", "肉类");
        when(productRepository.findAllActiveCategories()).thenReturn(categories);

        List<String> result = productService.getAllCategories();

        assertThat(result).isEqualTo(categories);
        verify(productRepository).findAllActiveCategories();
    }

    @Test
    @DisplayName("应该能删除商品(软删除)")
    void shouldDeleteProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(testProduct)).thenReturn(testProduct);

        productService.deleteProduct(1L);

        assertThat(testProduct.getActive()).isFalse();
        verify(productRepository).findById(1L);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("删除不存在的商品应该抛出异常")
    void shouldThrowExceptionWhenDeletingNonExistentProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found with id: 1");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("应该能批量更新商品状态")
    void shouldUpdateProductsStatus() {
        List<Long> productIds = List.of(1L, 2L, 3L);
        when(productRepository.updateActiveStatusByIds(false, productIds)).thenReturn(3);

        productService.updateProductsStatus(productIds, false);

        verify(productRepository).updateActiveStatusByIds(false, productIds);
    }

    @Test
    @DisplayName("应该能减少库存")
    void shouldDecreaseStock() {
        when(productRepository.decreaseStock(1L, 10)).thenReturn(1);

        productService.decreaseStock(1L, 10);

        verify(productRepository).decreaseStock(1L, 10);
    }

    @Test
    @DisplayName("库存不足时减少库存应该抛出异常")
    void shouldThrowExceptionWhenDecreasingInsufficientStock() {
        when(productRepository.decreaseStock(1L, 200)).thenReturn(0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> productService.decreaseStock(1L, 200))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient stock. Available: 100, Required: 200");

        verify(productRepository).decreaseStock(1L, 200);
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("减少不存在商品的库存应该抛出异常")
    void shouldThrowExceptionWhenDecreasingStockOfNonExistentProduct() {
        when(productRepository.decreaseStock(1L, 10)).thenReturn(0);
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.decreaseStock(1L, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found with id: 1");

        verify(productRepository).decreaseStock(1L, 10);
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("应该能增加库存")
    void shouldIncreaseStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.increaseStock(1L, 20)).thenReturn(1);

        productService.increaseStock(1L, 20);

        verify(productRepository).findById(1L);
        verify(productRepository).increaseStock(1L, 20);
    }

    @Test
    @DisplayName("增加不存在商品的库存应该抛出异常")
    void shouldThrowExceptionWhenIncreasingStockOfNonExistentProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.increaseStock(1L, 20))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found with id: 1");

        verify(productRepository).findById(1L);
        verify(productRepository, never()).increaseStock(anyLong(), any(Integer.class));
    }

    @Test
    @DisplayName("应该能获取低库存商品")
    void shouldGetLowStockProducts() {
        Product lowStockProduct = new Product();
        lowStockProduct.setId(2L);
        lowStockProduct.setName("库存较低商品");
        lowStockProduct.setStock(5);
        lowStockProduct.setActive(true);

        List<Product> lowStockProducts = List.of(lowStockProduct);
        when(productRepository.findByStockLessThanAndActiveTrue(10))
                .thenReturn(lowStockProducts);

        List<ProductResponse> result = productService.getLowStockProducts(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("库存较低商品");
        assertThat(result.get(0).getStock()).isEqualTo(5);

        verify(productRepository).findByStockLessThanAndActiveTrue(10);
    }
}