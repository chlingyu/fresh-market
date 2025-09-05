-- Align products table schema with Product entity
-- Version: 1.1.0
-- Date: 2025-09-05

-- Step 1: Drop the foreign key constraint on category_id
ALTER TABLE products DROP FOREIGN KEY products_ibfk_1;

-- Step 2: Drop the old category_id column
ALTER TABLE products DROP COLUMN category_id;

-- Step 3: Rename status column to active and change its type
ALTER TABLE products CHANGE COLUMN status active BOOLEAN NOT NULL DEFAULT TRUE;

-- Step 4: Add the new columns defined in the entity
ALTER TABLE products ADD COLUMN category VARCHAR(50) NOT NULL AFTER price;
ALTER TABLE products ADD COLUMN unit VARCHAR(20) AFTER stock;
ALTER TABLE products ADD COLUMN weight DECIMAL(8, 3) AFTER active;

-- Step 5: Update existing data for the new category column (example)
-- This assumes you want to set a default category for existing products.
-- In a real scenario, you might need a more complex data migration strategy.
UPDATE products SET category = '默认分类' WHERE category IS NULL;
