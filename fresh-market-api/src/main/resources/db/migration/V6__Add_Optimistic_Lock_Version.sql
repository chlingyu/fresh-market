-- 添加乐观锁版本字段以解决并发安全问题
-- Version: 6.0.0
-- Date: 2025-09-06

-- 为 products 表添加版本字段
ALTER TABLE products 
ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 更新现有记录的版本号为 0
UPDATE products SET version = 0 WHERE version IS NULL;

-- 为版本字段添加索引以提高查询性能
CREATE INDEX idx_products_version ON products(version);

-- 更新 products 表结构，对齐实体类字段
-- 修改 category_id 为可选，因为实体类中没有强制关联
ALTER TABLE products 
MODIFY COLUMN category_id BIGINT NULL,
ADD COLUMN category VARCHAR(50) COMMENT '商品分类名称',
ADD COLUMN unit VARCHAR(20) DEFAULT '个' COMMENT '商品单位',
ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否上架',
ADD COLUMN weight DECIMAL(8,3) COMMENT '商品重量(kg)';

-- 从原有的 category_id 关联表中迁移 category 名称
UPDATE products p 
INNER JOIN categories c ON p.category_id = c.id 
SET p.category = c.name;

-- 将 status 字段迁移到 active 字段
UPDATE products 
SET active = CASE 
    WHEN status = 'ACTIVE' THEN TRUE 
    ELSE FALSE 
END;

-- 创建索引优化查询性能
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_products_category ON products(category);

-- 添加字段注释
ALTER TABLE products 
MODIFY COLUMN name VARCHAR(100) NOT NULL COMMENT '商品名称',
MODIFY COLUMN description TEXT COMMENT '商品描述', 
MODIFY COLUMN price DECIMAL(10,2) NOT NULL COMMENT '商品价格',
MODIFY COLUMN stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
MODIFY COLUMN image_url VARCHAR(500) COMMENT '商品图片URL',
MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';