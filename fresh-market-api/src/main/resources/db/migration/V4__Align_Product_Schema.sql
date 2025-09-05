-- Align products table schema with Product entity
-- Version: 1.1.0 (Fixed)
-- Date: 2025-09-05

-- Check and add category column
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE table_name='products' AND column_name='category' AND table_schema=DATABASE()) = 0,
    'ALTER TABLE products ADD COLUMN category VARCHAR(50) DEFAULT ''fresh'' NOT NULL AFTER price',
    'SELECT ''category column already exists'' AS msg'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add unit column
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE table_name='products' AND column_name='unit' AND table_schema=DATABASE()) = 0,
    'ALTER TABLE products ADD COLUMN unit VARCHAR(20) DEFAULT ''kg'' AFTER stock',
    'SELECT ''unit column already exists'' AS msg'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add active column
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE table_name='products' AND column_name='active' AND table_schema=DATABASE()) = 0,
    'ALTER TABLE products ADD COLUMN active BOOLEAN DEFAULT TRUE NOT NULL AFTER unit',
    'SELECT ''active column already exists'' AS msg'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Update active column based on existing status values (if status column exists)
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE table_name='products' AND column_name='status' AND table_schema=DATABASE()) > 0,
    'UPDATE products SET active = (status = ''ACTIVE'')',
    'SELECT ''status column does not exist'' AS msg'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop the old status column if it exists
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE table_name='products' AND column_name='status' AND table_schema=DATABASE()) > 0,
    'ALTER TABLE products DROP COLUMN status',
    'SELECT ''status column already dropped'' AS msg'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add weight column
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE table_name='products' AND column_name='weight' AND table_schema=DATABASE()) = 0,
    'ALTER TABLE products ADD COLUMN weight DECIMAL(8, 3) DEFAULT 0.000 AFTER active',
    'SELECT ''weight column already exists'' AS msg'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
