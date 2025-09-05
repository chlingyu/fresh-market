package com.freshmarket.inventory.service.impl;

import com.freshmarket.inventory.service.InventoryService;
import com.freshmarket.product.entity.Product;
import com.freshmarket.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存服务实现
 */
@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final ProductRepository productRepository;

    public InventoryServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryCheckResult checkInventory(Map<Long, Integer> productQuantityMap) {
        logger.debug("Checking inventory for {} products", productQuantityMap.size());

        List<Long> productIds = new ArrayList<>(productQuantityMap.keySet());
        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            List<String> reasons = new ArrayList<>();
            reasons.add("部分商品不存在或已下架");
            return new InventoryCheckResult(false, Map.of(), reasons);
        }

        Map<Long, Product> productMap = products.stream()
                .filter(Product::getActive)
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<String> unavailableReasons = new ArrayList<>();
        Map<Long, Product> availableProducts = new HashMap<>();

        for (Map.Entry<Long, Integer> entry : productQuantityMap.entrySet()) {
            Long productId = entry.getKey();
            Integer requiredQuantity = entry.getValue();

            Product product = productMap.get(productId);
            if (product == null) {
                unavailableReasons.add(String.format("商品ID %d 不存在或已下架", productId));
                continue;
            }

            if (product.getStock() < requiredQuantity) {
                unavailableReasons.add(String.format("商品 %s 库存不足，可用库存：%d，需要：%d", 
                        product.getName(), product.getStock(), requiredQuantity));
                continue;
            }

            availableProducts.put(productId, product);
        }

        boolean allAvailable = unavailableReasons.isEmpty();
        return new InventoryCheckResult(allAvailable, availableProducts, unavailableReasons);
    }

    @Override
    public InventoryReservationResult reserveStock(List<InventoryReservation> reservations) {
        logger.debug("Reserving stock for {} items", reservations.size());

        List<InventoryReservation> successful = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (InventoryReservation reservation : reservations) {
            int updatedCount = productRepository.decreaseStockWithVersion(
                    reservation.productId(), 
                    reservation.quantity(),
                    reservation.version()
            );

            if (updatedCount > 0) {
                successful.add(reservation);
                logger.debug("Successfully reserved {} units of product {}", 
                        reservation.quantity(), reservation.productId());
            } else {
                String failureMsg = String.format("Failed to reserve %d units of product %s (ID: %d) - concurrent update or insufficient stock", 
                        reservation.quantity(), reservation.productName(), reservation.productId());
                failures.add(failureMsg);
                logger.warn(failureMsg);
            }
        }

        boolean success = failures.isEmpty();
        if (!success) {
            // 如果有失败的，需要回滚已成功的预扣
            logger.info("Rolling back {} successful reservations due to partial failure", successful.size());
            restoreStock(successful);
            return new InventoryReservationResult(false, List.of(), failures);
        }

        return new InventoryReservationResult(true, successful, List.of());
    }

    @Override
    public void restoreStock(List<InventoryReservation> reservations) {
        logger.debug("Restoring stock for {} items", reservations.size());

        for (InventoryReservation reservation : reservations) {
            // 恢复库存时不检查版本，因为我们要确保库存能被恢复
            int updatedCount = productRepository.increaseStock(
                    reservation.productId(), 
                    reservation.quantity()
            );

            if (updatedCount > 0) {
                logger.debug("Successfully restored {} units of product {}", 
                        reservation.quantity(), reservation.productId());
            } else {
                logger.error("Failed to restore {} units of product {} (ID: {})", 
                        reservation.quantity(), reservation.productName(), reservation.productId());
            }
        }
    }
}