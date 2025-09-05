package com.freshmarket.inventory.service;

import com.freshmarket.product.entity.Product;

import java.util.List;
import java.util.Map;

/**
 * 库存服务接口
 * 提供库存操作的抽象，解耦订单模块和商品模块
 */
public interface InventoryService {

    /**
     * 批量检查商品库存
     * @param productQuantityMap 商品ID和需要数量的映射
     * @return 库存检查结果
     */
    InventoryCheckResult checkInventory(Map<Long, Integer> productQuantityMap);

    /**
     * 预扣库存（原子操作）
     * @param reservations 库存预扣信息列表
     * @return 预扣结果
     */
    InventoryReservationResult reserveStock(List<InventoryReservation> reservations);

    /**
     * 恢复库存
     * @param reservations 需要恢复的库存信息列表
     */
    void restoreStock(List<InventoryReservation> reservations);

    /**
     * 库存检查结果
     */
    record InventoryCheckResult(
        boolean allAvailable,
        Map<Long, Product> availableProducts,
        List<String> unavailableReasons
    ) {}

    /**
     * 库存预扣信息
     */
    record InventoryReservation(
        Long productId,
        String productName,
        Integer quantity,
        Long version
    ) {}

    /**
     * 库存预扣结果
     */
    record InventoryReservationResult(
        boolean success,
        List<InventoryReservation> successfulReservations,
        List<String> failures
    ) {}
}