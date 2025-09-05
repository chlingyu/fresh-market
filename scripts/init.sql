-- Fresh Market Database Initialization Script
-- This script creates initial tables and sample data

SET FOREIGN_KEY_CHECKS = 0;

-- Drop tables if they exist (for clean restart)
DROP TABLE IF EXISTS task_queue;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS user_addresses;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- Create user_addresses table
CREATE TABLE user_addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(200) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址表';

-- Create categories table
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- Create products table  
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- Create cart_items table
CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_product (user_id, product_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- Create orders table
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'CREATED',
    address_name VARCHAR(50) NOT NULL,
    address_phone VARCHAR(20) NOT NULL,
    address_detail VARCHAR(200) NOT NULL,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- Create order_items table
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_image VARCHAR(500),
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

-- Create task_queue table (核心异步任务表)
CREATE TABLE task_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type VARCHAR(50) NOT NULL,
    business_id VARCHAR(100) NOT NULL,
    payload JSON,
    status VARCHAR(20) DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    max_retry INT DEFAULT 3,
    next_retry_time TIMESTAMP NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_status_next_retry (status, next_retry_time),
    INDEX idx_business_id (business_id),
    INDEX idx_task_type (task_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务队列表';

-- Insert sample data

-- Sample categories
INSERT INTO categories (name, parent_id, sort_order) VALUES 
('水果', NULL, 1),
('蔬菜', NULL, 2),
('肉类', NULL, 3),
('海鲜', NULL, 4),
('苹果类', 1, 1),
('柑橘类', 1, 2),
('叶菜类', 2, 1),
('根茎类', 2, 2);

-- Sample products
INSERT INTO products (category_id, name, description, price, stock, image_url, status) VALUES 
(5, '红富士苹果', '新鲜红富士苹果，香甜可口，产地烟台', 12.50, 100, 'https://example.com/images/apple.jpg', 'ACTIVE'),
(5, '青苹果', '清脆青苹果，酸甜适中，适合榨汁', 10.00, 80, 'https://example.com/images/green_apple.jpg', 'ACTIVE'),
(6, '新奇士橙子', '进口新奇士橙子，果肉饱满，维C丰富', 15.80, 60, 'https://example.com/images/orange.jpg', 'ACTIVE'),
(7, '有机小白菜', '有机种植小白菜，嫩绿鲜美', 8.00, 200, 'https://example.com/images/cabbage.jpg', 'ACTIVE'),
(7, '菠菜', '新鲜菠菜，富含铁质和维生素', 6.50, 150, 'https://example.com/images/spinach.jpg', 'ACTIVE'),
(8, '土豆', '新鲜土豆，口感绵软，适合多种烹饪', 4.50, 300, 'https://example.com/images/potato.jpg', 'ACTIVE'),
(3, '新鲜猪肉', '优质猪肉，肉质鲜美，当日配送', 28.80, 50, 'https://example.com/images/pork.jpg', 'ACTIVE'),
(4, '基围虾', '新鲜基围虾，肉质Q弹，营养丰富', 45.00, 30, 'https://example.com/images/shrimp.jpg', 'ACTIVE');

-- Sample user (password: password123, bcrypt encoded)
INSERT INTO users (username, email, password_hash, phone) VALUES 
('demo_user', 'demo@freshmarket.com', '$2a$10$Nt.5/ZjIEJdRYlgQdh9x2eFPPNksP3sMD/Qb1cRyX7LZHmC8GvWMe', '13812345678');

-- Sample user address
INSERT INTO user_addresses (user_id, name, phone, address, is_default) VALUES 
(1, '张三', '13812345678', '北京市朝阳区望京SOHO塔1座1001室', TRUE);

SET FOREIGN_KEY_CHECKS = 1;