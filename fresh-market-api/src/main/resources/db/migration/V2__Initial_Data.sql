-- Fresh Market Database Initial Data
-- Version: 2.0.0
-- Date: 2025-09-05

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