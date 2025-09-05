package com.freshmarket.common.exception;

/**
 * 库存不足异常
 */
public class InsufficientStockException extends BusinessException {
    
    private final Long productId;
    private final String productName;
    private final Integer requestedQuantity;
    private final Integer availableQuantity;

    public InsufficientStockException(Long productId, String productName, 
                                    Integer requestedQuantity, Integer availableQuantity) {
        super("INSUFFICIENT_STOCK", 
              String.format("商品 %s (ID: %d) 库存不足，需要 %d 件，可用 %d 件", 
                          productName, productId, requestedQuantity, availableQuantity));
        this.productId = productId;
        this.productName = productName;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
}