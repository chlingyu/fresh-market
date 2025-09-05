package com.freshmarket.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmarket.common.exception.ResourceNotFoundException;
import com.freshmarket.product.dto.ProductRequest;
import com.freshmarket.product.dto.ProductResponse;
import com.freshmarket.product.dto.ProductSearchRequest;
import com.freshmarket.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 商品Controller测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("商品Controller测试")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductResponse testProductResponse;
    private ProductRequest testProductRequest;

    @BeforeEach
    void setUp() {
        testProductResponse = new ProductResponse();
        testProductResponse.setId(1L);
        testProductResponse.setName("测试苹果");
        testProductResponse.setDescription("测试描述");
        testProductResponse.setPrice(new BigDecimal("12.50"));
        testProductResponse.setCategory("水果");
        testProductResponse.setStock(100);
        testProductResponse.setUnit("斤");
        testProductResponse.setActive(true);
        testProductResponse.setWeight(new BigDecimal("0.500"));
        testProductResponse.setCreatedAt(Instant.now());
        testProductResponse.setUpdatedAt(Instant.now());

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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("应该能创建商品")
    void shouldCreateProduct() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(testProductResponse);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProductRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("测试苹果")))
                .andExpect(jsonPath("$.data.price", is(12.50)))
                .andExpect(jsonPath("$.data.category", is("水果")))
                .andExpect(jsonPath("$.data.stock", is(100)));

        verify(productService).createProduct(any(ProductRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("创建商品时验证失败应该返回400")
    void shouldReturn400WhenCreateProductWithInvalidData() throws Exception {
        ProductRequest invalidRequest = new ProductRequest();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("应该能获取商品详情")
    void shouldGetProduct() throws Exception {
        when(productService.getProductById(1L)).thenReturn(testProductResponse);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("测试苹果")));

        verify(productService).getProductById(1L);
    }

    @Test
    @DisplayName("获取不存在的商品应该返回404")
    void shouldReturn404WhenGetNonExistentProduct() throws Exception {
        when(productService.getProductById(1L)).thenThrow(new ResourceNotFoundException("Product not found with id: 1"));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("应该能更新商品")
    void shouldUpdateProduct() throws Exception {
        testProductResponse.setName("更新后的苹果");
        when(productService.updateProduct(eq(1L), any(ProductRequest.class))).thenReturn(testProductResponse);

        testProductRequest.setName("更新后的苹果");

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProductRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("更新后的苹果")));

        verify(productService).updateProduct(eq(1L), any(ProductRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("应该能删除商品")
    void shouldDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(productService).deleteProduct(1L);
    }

    @Test
    @DisplayName("应该能搜索商品")
    void shouldSearchProducts() throws Exception {
        Page<ProductResponse> productPage = new PageImpl<>(
                List.of(testProductResponse), 
                PageRequest.of(0, 20), 
                1
        );
        when(productService.searchProducts(any(ProductSearchRequest.class))).thenReturn(productPage);

        mockMvc.perform(get("/api/v1/products")
                        .param("name", "苹果")
                        .param("category", "水果")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("测试苹果")));

        verify(productService).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("应该能获取所有分类")
    void shouldGetAllCategories() throws Exception {
        List<String> categories = List.of("水果", "蔬菜", "肉类");
        when(productService.getAllCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/v1/products/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0]", is("水果")));

        verify(productService).getAllCategories();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("应该能减少库存")
    void shouldDecreaseStock() throws Exception {
        doNothing().when(productService).decreaseStock(1L, 10);

        mockMvc.perform(patch("/api/v1/products/1/stock/decrease")
                        .param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(productService).decreaseStock(1L, 10);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("库存不足时减少库存应该返回400")
    void shouldReturn400WhenDecreaseInsufficientStock() throws Exception {
        doThrow(new IllegalStateException("Insufficient stock")).when(productService).decreaseStock(1L, 200);

        mockMvc.perform(patch("/api/v1/products/1/stock/decrease")
                        .param("quantity", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("应该能增加库存")
    void shouldIncreaseStock() throws Exception {
        doNothing().when(productService).increaseStock(1L, 20);

        mockMvc.perform(patch("/api/v1/products/1/stock/increase")
                        .param("quantity", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(productService).increaseStock(1L, 20);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("应该能获取低库存商品")
    void shouldGetLowStockProducts() throws Exception {
        List<ProductResponse> lowStockProducts = List.of(testProductResponse);
        when(productService.getLowStockProducts(anyInt())).thenReturn(lowStockProducts);

        mockMvc.perform(get("/api/v1/products/low-stock")
                        .param("threshold", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(productService).getLowStockProducts(10);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("应该能批量更新商品状态")
    void shouldUpdateProductsStatus() throws Exception {
        doNothing().when(productService).updateProductsStatus(any(List.class), eq(false));

        mockMvc.perform(patch("/api/v1/products/batch/status")
                        .param("ids", "1,2,3")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(productService).updateProductsStatus(any(List.class), eq(false));
    }

    @Test
    @DisplayName("无权限时访问管理员API应该返回403")
    void shouldReturn403WhenAccessingAdminEndpointsWithoutPermission() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProductRequest)))
                .andExpect(status().isUnauthorized());
    }
}