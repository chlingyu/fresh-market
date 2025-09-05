package com.freshmarket.product.repository;

import com.freshmarket.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品Repository测试
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("商品Repository测试")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Product activeApple;
    private Product activeBanana;
    private Product inactiveOrange;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        activeApple = new Product();
        activeApple.setName("红富士苹果");
        activeApple.setDescription("新鲜的红富士苹果");
        activeApple.setPrice(new BigDecimal("12.50"));
        activeApple.setCategory("水果");
        activeApple.setStock(100);
        activeApple.setUnit("斤");
        activeApple.setActive(true);
        activeApple.setWeight(new BigDecimal("0.500"));

        activeBanana = new Product();
        activeBanana.setName("香蕉");
        activeBanana.setDescription("进口香蕉");
        activeBanana.setPrice(new BigDecimal("8.00"));
        activeBanana.setCategory("水果");
        activeBanana.setStock(5);
        activeBanana.setUnit("斤");
        activeBanana.setActive(true);
        activeBanana.setWeight(new BigDecimal("0.300"));

        inactiveOrange = new Product();
        inactiveOrange.setName("橙子");
        inactiveOrange.setDescription("下架的橙子");
        inactiveOrange.setPrice(new BigDecimal("10.00"));
        inactiveOrange.setCategory("水果");
        inactiveOrange.setStock(50);
        inactiveOrange.setUnit("斤");
        inactiveOrange.setActive(false);
        inactiveOrange.setWeight(new BigDecimal("0.400"));

        productRepository.save(activeApple);
        productRepository.save(activeBanana);
        productRepository.save(inactiveOrange);
    }

    @Test
    @DisplayName("应该能根据分类查询上架商品")
    void shouldFindByCategoryAndActiveTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> products = productRepository.findByCategoryAndActiveTrue("水果", pageable);

        assertThat(products.getContent()).hasSize(2);
        assertThat(products.getContent()).containsExactlyInAnyOrder(activeApple, activeBanana);
    }

    @Test
    @DisplayName("应该能查询所有上架商品")
    void shouldFindByActiveTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> products = productRepository.findByActiveTrue(pageable);

        assertThat(products.getContent()).hasSize(2);
        assertThat(products.getContent()).allMatch(Product::getActive);
    }

    @Test
    @DisplayName("应该能根据名称模糊查询上架商品")
    void shouldFindByNameContainingIgnoreCaseAndActiveTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue("苹果", pageable);

        assertThat(products.getContent()).hasSize(1);
        assertThat(products.getContent().get(0)).isEqualTo(activeApple);
    }

    @Test
    @DisplayName("应该能根据价格区间查询上架商品")
    void shouldFindByPriceBetweenAndActiveTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> products = productRepository.findByPriceBetweenAndActiveTrue(
                new BigDecimal("10.00"), new BigDecimal("15.00"), pageable);

        assertThat(products.getContent()).hasSize(1);
        assertThat(products.getContent().get(0)).isEqualTo(activeApple);
    }

    @Test
    @DisplayName("应该能根据分类和价格区间查询上架商品")
    void shouldFindByCategoryAndPriceBetweenAndActiveTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> products = productRepository.findByCategoryAndPriceBetweenAndActiveTrue(
                "水果", new BigDecimal("7.00"), new BigDecimal("9.00"), pageable);

        assertThat(products.getContent()).hasSize(1);
        assertThat(products.getContent().get(0)).isEqualTo(activeBanana);
    }

    @Test
    @DisplayName("应该能查询库存低于指定数量的商品")
    void shouldFindByStockLessThanAndActiveTrue() {
        List<Product> products = productRepository.findByStockLessThanAndActiveTrue(10);

        assertThat(products).hasSize(1);
        assertThat(products.get(0)).isEqualTo(activeBanana);
    }

    @Test
    @DisplayName("应该能根据分类统计上架商品数量")
    void shouldCountByCategoryAndActiveTrue() {
        long count = productRepository.countByCategoryAndActiveTrue("水果");

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("应该能查询所有商品分类")
    void shouldFindAllActiveCategories() {
        List<String> categories = productRepository.findAllActiveCategories();

        assertThat(categories).containsExactly("水果");
    }

    @Test
    @DisplayName("应该能根据ID查询上架商品")
    void shouldFindByIdAndActiveTrue() {
        Optional<Product> product = productRepository.findByIdAndActiveTrue(activeApple.getId());

        assertThat(product).isPresent();
        assertThat(product.get()).isEqualTo(activeApple);

        Optional<Product> inactiveProduct = productRepository.findByIdAndActiveTrue(inactiveOrange.getId());
        assertThat(inactiveProduct).isEmpty();
    }

    @Test
    @DisplayName("应该能批量更新商品状态")
    void shouldUpdateActiveStatusByIds() {
        List<Long> ids = List.of(activeApple.getId(), activeBanana.getId());
        int updatedCount = productRepository.updateActiveStatusByIds(false, ids);

        assertThat(updatedCount).isEqualTo(2);

        // 刷新持久化上下文
        entityManager.flush();
        entityManager.clear();

        // 验证更新结果
        Optional<Product> apple = productRepository.findById(activeApple.getId());
        Optional<Product> banana = productRepository.findById(activeBanana.getId());

        assertThat(apple).isPresent();
        assertThat(apple.get().getActive()).isFalse();
        assertThat(banana).isPresent();
        assertThat(banana.get().getActive()).isFalse();
    }

    @Test
    @DisplayName("应该能减少库存")
    void shouldDecreaseStock() {
        int updatedCount = productRepository.decreaseStock(activeApple.getId(), 10);

        assertThat(updatedCount).isEqualTo(1);

        // 刷新持久化上下文
        entityManager.flush();
        entityManager.clear();

        Optional<Product> product = productRepository.findById(activeApple.getId());
        assertThat(product).isPresent();
        assertThat(product.get().getStock()).isEqualTo(90);
    }

    @Test
    @DisplayName("库存不足时不应该减少库存")
    void shouldNotDecreaseStockWhenInsufficient() {
        int updatedCount = productRepository.decreaseStock(activeBanana.getId(), 10);

        assertThat(updatedCount).isEqualTo(0);

        Optional<Product> product = productRepository.findById(activeBanana.getId());
        assertThat(product).isPresent();
        assertThat(product.get().getStock()).isEqualTo(5); // 库存应该保持不变
    }

    @Test
    @DisplayName("应该能增加库存")
    void shouldIncreaseStock() {
        int updatedCount = productRepository.increaseStock(activeApple.getId(), 20);

        assertThat(updatedCount).isEqualTo(1);

        // 刷新持久化上下文
        entityManager.flush();
        entityManager.clear();

        Optional<Product> product = productRepository.findById(activeApple.getId());
        assertThat(product).isPresent();
        assertThat(product.get().getStock()).isEqualTo(120);
    }
}