-- Fresh Market Database - Payment Table
-- Version: 3.0.0 
-- Date: 2025-09-05
-- Description: 添加支付记录表

-- Create payments table
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway VARCHAR(20) NOT NULL,
    transaction_id VARCHAR(64),
    payment_number VARCHAR(32) NOT NULL UNIQUE,
    gateway_response TEXT,
    failure_reason VARCHAR(500),
    paid_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 外键约束
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_gateway (gateway),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_created_at (created_at),
    INDEX idx_payment_number (payment_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';